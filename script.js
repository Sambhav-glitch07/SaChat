
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import {
 getAuth,onAuthStateChanged,createUserWithEmailAndPassword,
 signInWithEmailAndPassword,signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
 getFirestore,doc,setDoc,getDoc,collection,addDoc,
 query,where,orderBy,onSnapshot,serverTimestamp
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";

const firebaseConfig={
 apiKey:"AIzaSyAs2cxe_h5vurR0U3-PNp9q300RqYlPrYU",
 authDomain:"sachat-a705d.firebaseapp.com",
 projectId:"sachat-a705d",
 storageBucket:"sachat-a705d.firebasestorage.app",
 messagingSenderId:"140263915737",
 appId:"1:140263915737:web:32654e1de6c5a653f57ec8",
 measurementId:"G-H6H1XTJWZQ"
};

const app=initializeApp(firebaseConfig);
const auth=getAuth(app);
const db=getFirestore(app);
const $=id=>document.getElementById(id);
const verifiedEmail="sambhavgupta997@gmail.com";

let current=null,activeUid=null,stopUsers=null,stopMessages=null;
let knownMessages=new Set();

function verified(email){
 return email?.toLowerCase()===verifiedEmail;
}
function badge(email){
 return verified(email)?'<span class="verified" title="SaChat verified">✓</span>':"";
}
function chatId(a,b){return [a,b].sort().join("_");}
function showError(msg){$("authError").textContent=msg;}

$("register").onclick=async()=>{
 try{
  const name=$("displayName").value.trim();
  const email=$("email").value.trim();
  const password=$("password").value;
  if(!name||!email||password.length<6)throw Error("Enter your name, email and password (6+ characters).");
  const result=await createUserWithEmailAndPassword(auth,email,password);
  await setDoc(doc(db,"users",result.user.uid),{
   uid:result.user.uid,name,email:email.toLowerCase(),
   createdAt:serverTimestamp(),lastSeen:serverTimestamp()
  });
 }catch(e){showError(e.message);}
};

$("login").onclick=async()=>{
 try{
  await signInWithEmailAndPassword(auth,$("email").value.trim(),$("password").value);
 }catch(e){showError(e.message);}
};

$("logout").onclick=()=>signOut(auth);

onAuthStateChanged(auth,async user=>{
 current=user;
 if(!user){
  $("authScreen").classList.remove("hidden");
  $("myName").textContent="My account";
  $("chatScreen").classList.add("hidden");
  $("welcome").classList.remove("hidden");
  $("people").innerHTML="";
  if(stopUsers)stopUsers();
  if(stopMessages)stopMessages();
  return;
 }
 try{
  const ref=doc(db,"users",user.uid);
  const snap=await getDoc(ref);
  if(!snap.exists()){
   await setDoc(ref,{
    uid:user.uid,name:user.email.split("@")[0],
    email:user.email.toLowerCase(),createdAt:serverTimestamp()
   },{merge:true});
  }
  $("authScreen").classList.add("hidden");
  $("myName").textContent=user.email.split("@")[0];
  loadPeople();
 }catch(e){showError(e.message);}
});

function loadPeople(){
 if(stopUsers)stopUsers();
 stopUsers=onSnapshot(collection(db,"users"),snapshot=>{
  const users=snapshot.docs
   .map(d=>d.data())
   .filter(u=>u.uid!==current.uid);
  renderPeople(users);
 },e=>showError(e.message));
}

function renderPeople(users){
 const term=$("search").value.toLowerCase();
 $("people").innerHTML="";
 users.filter(u=>(u.name+" "+u.email).toLowerCase().includes(term))
 .forEach(u=>{
  const row=document.createElement("div");
  row.className="person"+(activeUid===u.uid?" active":"");
  const avatar=document.createElement("div");
  avatar.className="avatar";
  avatar.textContent=(u.name||"S")[0].toUpperCase();
  const details=document.createElement("div");
  details.className="person-info";
  const name=document.createElement("strong");
  name.textContent=u.name||"SaChat member";
  name.innerHTML=name.textContent+badge(u.email);
  const email=document.createElement("small");
  email.textContent=u.email||"";
  details.append(name,email);
  row.append(avatar,details);
  row.onclick=()=>openChat(u);
  $("people").append(row);
 });
}
$("search").addEventListener("input",()=>{
 if(current){
  getDocsOnce();
 }
});
let cachedUsers=[];
function getDocsOnce(){renderPeople(cachedUsers);}

async function openChat(person){
 activeUid=person.uid;
 $("welcome").classList.add("hidden");
 $("chatScreen").classList.remove("hidden");
 $("chatName").innerHTML=(person.name||"SaChat member")+badge(person.email);
 $("chatAvatar").textContent=(person.name||"S")[0].toUpperCase();
 $("app")?.classList.add("chat-open");
 document.querySelector(".app").classList.add("chat-open");
 knownMessages=new Set();
 if(stopMessages)stopMessages();

 const id=chatId(current.uid,person.uid);
 const q=query(collection(db,"chats",id,"messages"),orderBy("createdAt","asc"));
 stopMessages=onSnapshot(q,snapshot=>{
  const box=$("messages");
  const wasAtBottom=box.scrollHeight-box.scrollTop-box.clientHeight<100;
  snapshot.docChanges().forEach(change=>{
   if(change.type==="added")knownMessages.add(change.doc.id);
  });
  box.innerHTML="";
  snapshot.docs.forEach(d=>{
   const m=d.data();
   const mine=m.senderId===current.uid;
   const bubble=document.createElement("div");
   bubble.className="bubble"+(mine?" mine":"");
   bubble.textContent=m.text||"";
   if(!knownMessages.has(d.id))bubble.classList.add("new");
   const time=document.createElement("small");
   time.textContent=m.createdAt?.toDate?.().toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"})||"";
   bubble.append(time);
   box.append(bubble);
  });
  if(wasAtBottom||snapshot.metadata.hasPendingWrites)box.scrollTop=box.scrollHeight;
 },e=>showError(e.message));
}

$("composer").onsubmit=async e=>{
 e.preventDefault();
 const text=$("messageInput").value.trim();
 if(!text||!current||!activeUid)return;
 $("messageInput").value="";
 const id=chatId(current.uid,activeUid);
 try{
  await setDoc(doc(db,"chats",id),{
   members:[current.uid,activeUid],
   lastMessage:text,
   updatedAt:serverTimestamp()
  },{merge:true});
  await addDoc(collection(db,"chats",id,"messages"),{
   senderId:current.uid,receiverId:activeUid,
   text,createdAt:serverTimestamp()
  });
 }catch(error){
  $("messageInput").value=text;
  showError(error.message);
 }
};

const originalLoadPeople=loadPeople;
loadPeople=function(){
 if(stopUsers)stopUsers();
 stopUsers=onSnapshot(collection(db,"users"),snapshot=>{
  cachedUsers=snapshot.docs.map(d=>d.data()).filter(u=>u.uid!==current.uid);
  renderPeople(cachedUsers);
 },e=>showError(e.message));
};
