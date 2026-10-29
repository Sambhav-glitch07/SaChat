package in.sachat.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainActivity : Activity() {
    private val bg = Color.rgb(11, 15, 27)
    private val panel = Color.rgb(20, 27, 44)
    private val panel2 = Color.rgb(28, 37, 59)
    private val mint = Color.rgb(91, 231, 190)
    private val textMain = Color.rgb(242, 246, 255)
    private val textMuted = Color.rgb(151, 164, 190)
    private val line = Color.argb(75, 191, 211, 255)

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var activeChatId: String? = null
    private var activeChatName: String = ""
    private var activeOtherUid: String = ""
    private var listener: ListenerRegistration? = null
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        if (auth.currentUser == null) showAuth() else showHome()
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    private fun glass(color: Int = Color.argb(190, 24, 33, 53), radius: Float = 22f): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color, Color.argb(220, 24, 31, 49), Color.argb(200, 18, 25, 42))
        ).apply {
            cornerRadius = radius
            setStroke(1, line)
        }

    private fun pill(color: Int, radius: Float = 30f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun base(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 12)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(13, 19, 35), Color.rgb(20, 22, 43), Color.rgb(8, 17, 30))
            )
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        setContentView(root)
        return root
    }

    private fun label(value: String, size: Float, color: Int = textMain, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun field(hintText: String, password: Boolean = false): EditText =
        EditText(this).apply {
            hint = hintText
            setHintTextColor(textMuted)
            textSize = 15f
            setTextColor(textMain)
            singleLine = true
            setPadding(18, 14, 18, 14)
            background = glass(Color.argb(150, 31, 42, 65), 18f)
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else if (hintText.contains("email", true)) InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                else InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

    private fun actionButton(title: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (title.contains("Send", true) || title.contains("Create", true) || title.contains("Sign in", true)) bg else textMain)
            gravity = Gravity.CENTER
            setPadding(20, 13, 20, 13)
            background = pill(if (title.contains("Send", true) || title.contains("Create", true) || title.contains("Sign in", true)) mint else Color.argb(100, 55, 73, 106), 18f)
            setOnClickListener { onClick() }
        }

    private fun showAuth() {
        listener?.remove()
        val page = base()
        page.gravity = Gravity.CENTER_VERTICAL
        page.setPadding(22, 24, 22, 24)

        val mark = TextView(this).apply {
            text = "S"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(bg)
            gravity = Gravity.CENTER
            background = pill(mint, 20f)
        }
        page.addView(mark, LinearLayout.LayoutParams(58, 58).apply { bottomMargin = 22 })
        page.addView(label("SaChat", 34f, textMain, true))
        page.addView(label("Your conversations, in a different light.", 14f, textMuted).apply {
            setPadding(0, 4, 0, 28)
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 18)
            background = glass(Color.argb(180, 23, 32, 52), 28f)
        }
        val name = field("Display name")
        val email = field("Email address")
        val password = field("Password (6+ characters)", true)
        card.addView(label("WELCOME BACK", 11f, mint, true).apply { setPadding(2, 0, 0, 16) })
        card.addView(name)
        card.addView(email)
        card.addView(password)
        val status = label("", 12f, Color.rgb(255, 135, 151)).apply { setPadding(2, 10, 2, 10) }
        card.addView(actionButton("Create account") {
            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (n.isBlank() || e.isBlank() || p.length < 6) {
                status.text = "Enter a display name, email and password of at least 6 characters."
            } else {
                status.text = "Creating account…"
                auth.createUserWithEmailAndPassword(e, p).addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    db.collection("users").document(uid).set(
                        mapOf("uid" to uid, "name" to n, "email" to e,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "lastSeen" to FieldValue.serverTimestamp())
                    ).addOnSuccessListener { showHome() }
                     .addOnFailureListener { status.text = "Profile save failed: ${it.localizedMessage}" }
                }.addOnFailureListener { status.text = it.localizedMessage ?: "Could not create account." }
            }
        }.apply { layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 } })
        card.addView(actionButton("Sign in") {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (e.isBlank() || p.isBlank()) status.text = "Enter your email and password."
            else {
                status.text = "Signing in…"
                auth.signInWithEmailAndPassword(e, p).addOnSuccessListener {
                    val user = auth.currentUser ?: return@addOnSuccessListener
                    db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            db.collection("users").document(user.uid).set(
                                mapOf("uid" to user.uid, "name" to (user.displayName ?: e.substringBefore("@")),
                                    "email" to e, "createdAt" to FieldValue.serverTimestamp())
                            ).addOnCompleteListener { showHome() }
                        } else showHome()
                    }
                }.addOnFailureListener { status.text = it.localizedMessage ?: "Sign in failed." }
            }
        }.apply { layoutParams = LinearLayout.LayoutParams(-1, -2) })
        card.addView(status)
        page.addView(card)
        page.addView(label("PRIVATE BY DESIGN  •  CONNECTED THROUGH FIREBASE", 10f, textMuted).apply {
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 0)
        })
    }

    private fun showHome() {
        listener?.remove()
        activeChatId = null
        val page = base()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 14, 14, 14)
            background = glass(Color.argb(180, 26, 38, 59), 24f)
        }
        val logo = TextView(this).apply {
            text = "S"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(bg)
            gravity = Gravity.CENTER
            background = pill(mint, 15f)
        }
        header.addView(logo, LinearLayout.LayoutParams(42, 42))
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 0, 0, 0)
        }
        titleBox.addView(label("SaChat", 22f, textMain, true))
        titleBox.addView(label("MESSAGES • ONLINE", 10f, mint, true))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("⋯", 28f, textMain).apply {
            gravity = Gravity.CENTER
            setPadding(12, 0, 8, 0)
            setOnClickListener { showAccountMenu() }
        })
        page.addView(header)

        val search = EditText(this).apply {
            hint = "Search conversations or people"
            setHintTextColor(textMuted)
            setTextColor(textMain)
            textSize = 14f
            singleLine = true
            setPadding(18, 13, 18, 13)
            background = glass(Color.argb(125, 29, 40, 62), 18f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 14, 0, 12)
            }
        }
        page.addView(search)
        page.addView(label("YOUR SPACE", 11f, textMuted, true).apply {
            setPadding(4, 2, 4, 10)
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { clipToPadding = false }
        scroll.addView(list)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            background = glass(Color.argb(190, 23, 33, 53), 22f)
        }
        val current = auth.currentUser ?: run { showAuth(); return }
        val myInitial = (current.email?.firstOrNull() ?: 'S').uppercaseChar().toString()
        bottom.addView(TextView(this).apply {
            text = myInitial
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(bg)
            setTypeface(null, Typeface.BOLD)
            background = pill(mint, 30f)
        }, LinearLayout.LayoutParams(38, 38))
        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 0, 0, 0)
        }
        profile.addView(label(current.email?.substringBefore("@") ?: "Your profile", 14f, textMain, true))
        profile.addView(label("Account connected", 11f, mint))
        bottom.addView(profile, LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(label("SETTINGS", 10f, textMuted, true).apply { setOnClickListener { showAccountMenu() } })
        page.addView(bottom, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })

        db.collection("users").document(current.uid).update("lastSeen", FieldValue.serverTimestamp())
        db.collection("users").get().addOnSuccessListener { snapshot ->
            list.removeAllViews()
            val docs = snapshot.documents.filter { it.id != current.uid }
            if (docs.isEmpty()) {
                val empty = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(22, 42, 22, 42)
                    background = glass(Color.argb(115, 28, 39, 60), 26f)
                }
                empty.addView(label("No conversations yet", 18f, textMain, true).apply { gravity = Gravity.CENTER })
                empty.addView(label("Create another account on a second device to start chatting.", 13f, textMuted).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, 8, 0, 0)
                })
                list.addView(empty)
            }
            docs.forEach { doc ->
                val personName = doc.getString("name") ?: "SaChat user"
                val personEmail = doc.getString("email") ?: ""
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(14, 13, 14, 13)
                    background = glass(Color.argb(135, 27, 37, 58), 22f)
                }
                val avatar = TextView(this).apply {
                    text = personName.take(1).uppercase()
                    textSize = 19f
                    setTextColor(mint)
                    gravity = Gravity.CENTER
                    background = pill(Color.argb(130, 48, 74, 91), 30f)
                }
                item.addView(avatar, LinearLayout.LayoutParams(48, 48))
                val details = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(13, 0, 0, 0)
                }
                details.addView(label(personName, 16f, textMain, true))
                details.addView(label(personEmail, 12f, textMuted))
                item.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
                item.addView(label("CHAT", 10f, mint, true))
                item.setOnClickListener { openChat(doc.id, personName) }
                val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 9 }
                list.addView(item, lp)
            }
            search.addTextChangedListener(SimpleTextWatcher { q ->
                for (i in 0 until list.childCount) {
                    val child = list.getChildAt(i)
                    if (child is LinearLayout && child.childCount > 1) {
                        val title = (child.getChildAt(1) as? LinearLayout)?.let {
                            (it.getChildAt(0) as? TextView)?.text?.toString()
                        } ?: ""
                        child.visibility = if (title.contains(q, true) || q.isBlank()) View.VISIBLE else View.GONE
                    }
                }
            })
        }.addOnFailureListener {
            list.addView(label("Could not load people. Check Firebase setup and Firestore rules.", 14f, Color.rgb(255, 135, 151)))
        }
    }

    private fun showAccountMenu() {
        val options = arrayOf("Account details", "Sign out")
        android.app.AlertDialog.Builder(this)
            .setTitle("SaChat settings")
            .setItems(options) { _, which ->
                if (which == 1) {
                    auth.signOut()
                    showAuth()
                } else {
                    val user = auth.currentUser
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Account")
                        .setMessage(user?.email ?: "Signed out")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }.show()
    }

    private fun chatId(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    private fun openChat(otherUid: String, otherName: String) {
        val currentUid = auth.currentUser?.uid ?: return
        activeChatId = chatId(currentUid, otherUid)
        activeChatName = otherName
        activeOtherUid = otherUid
        showConversation(currentUid, otherUid)
    }

    private fun showConversation(currentUid: String, otherUid: String) {
        listener?.remove()
        val chat = activeChatId ?: return
        val page = base()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 12, 14, 12)
            background = glass(Color.argb(185, 26, 38, 59), 22f)
        }
        header.addView(label("‹", 32f, textMain).apply {
            setPadding(8, 0, 14, 0)
            setOnClickListener { showHome() }
        })
        val avatar = TextView(this).apply {
            text = activeChatName.take(1).uppercase()
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(bg)
            background = pill(mint, 30f)
        }
        header.addView(avatar, LinearLayout.LayoutParams(40, 40))
        val nameBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(11, 0, 0, 0)
        }
        nameBlock.addView(label(activeChatName, 16f, textMain, true))
        nameBlock.addView(label("SAChat DIRECT MESSAGE", 10f, mint, true))
        header.addView(nameBlock, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("⋯", 26f, textMain).apply {
            setPadding(10, 0, 4, 0)
            setOnClickListener { showAccountMenu() }
        })
        page.addView(header)

        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 14, 4, 14)
        }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setPadding(0, 8, 0, 8)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(14, 20, 34), Color.rgb(18, 24, 40), Color.rgb(12, 20, 34))
            ).apply { cornerRadius = 24f }
        }
        scroll.addView(messages)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 12; bottomMargin = 12 })

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 7, 7, 7)
            background = glass(Color.argb(205, 27, 38, 60), 24f)
        }
        val input = EditText(this).apply {
            hint = "Message SaChat"
            setHintTextColor(textMuted)
            setTextColor(textMain)
            textSize = 15f
            minLines = 1
            maxLines = 4
            setPadding(15, 9, 12, 9)
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(actionButton("Send") {
            val body = input.text.toString().trim()
            if (body.isNotEmpty()) {
                val messageRef = db.collection("chats").document(chat).collection("messages").document()
                val payload = mapOf(
                    "id" to messageRef.id,
                    "chatId" to chat,
                    "senderId" to currentUid,
                    "receiverId" to otherUid,
                    "text" to body,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("chats").document(chat).set(
                    mapOf("members" to listOf(currentUid, otherUid), "lastMessage" to body,
                        "updatedAt" to FieldValue.serverTimestamp()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).addOnSuccessListener {
                    messageRef.set(payload).addOnSuccessListener { input.setText("") }
                        .addOnFailureListener { toast("Message failed: ${it.localizedMessage}") }
                }.addOnFailureListener { toast("Could not send: ${it.localizedMessage}") }
            }
        })
        page.addView(composer)

        listener = db.collection("chats").document(chat).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { toast("Messages error: ${error.localizedMessage}"); return@addSnapshotListener }
                messages.removeAllViews()
                snapshot?.documents?.forEach { doc ->
                    val sender = doc.getString("senderId")
                    val body = doc.getString("text") ?: ""
                    val bubble = TextView(this).apply {
                        text = body
                        textSize = 15f
                        setTextColor(textMain)
                        setPadding(15, 11, 15, 11)
                        background = glass(
                            if (sender == currentUid) Color.argb(205, 35, 102, 92)
                            else Color.argb(185, 35, 45, 66), 18f
                        )
                        maxWidth = (resources.displayMetrics.widthPixels * 0.76f).toInt()
                    }
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = if (sender == currentUid) Gravity.END else Gravity.START
                        setPadding(5, 4, 5, 4)
                    }
                    row.addView(bubble)
                    messages.addView(row)
                }
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
