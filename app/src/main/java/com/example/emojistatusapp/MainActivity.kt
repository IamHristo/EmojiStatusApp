package com.example.emojistatusapp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.emojistatusapp.ui.login.LoginActivity
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


data class User(
    val displayName: String = "",
    val emojis: String = "",
)

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class MainActivity : AppCompatActivity() {

    private companion object
    {
        private const val TAG = "MainActivity"
    }
    // Access a Cloud Firestore instance from your Activity
    private val db = Firebase.firestore
    private lateinit var rvUsers: RecyclerView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvUsers = findViewById(R.id.rvUsers)

        auth = Firebase.auth
        val query = db.collection("users")

        val test = query
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        val options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = object: FirestoreRecyclerAdapter<User, UserViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                val view = LayoutInflater.from(this@MainActivity).inflate(android.R.layout.simple_list_item_2, parent, false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvEmojis: TextView = holder.itemView.findViewById(android.R.id.text2)

                tvName.text = model.displayName
                tvEmojis.text = model.emojis
            }

            override fun onError(e: FirebaseFirestoreException) {
                Log.d(TAG, "Error - " + e.toString())
            }

        }

        if(rvUsers != null)
        {
            rvUsers.layoutManager = LinearLayoutManager(this)
            rvUsers.adapter = adapter
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.miLogout)
        {
            Log.i(TAG, "Logout")
            auth.signOut()
            val logoutIntent = Intent(this, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity((logoutIntent))
        }
        else if(item.itemId == R.id.miEdit)
        {
            Log.i(TAG, "Show alert dialog to edit status")
            showAlertDialog()
        }
        else if(item.itemId == R.id.miReferral)
        {
            val myFragment = ReferralFragment()
            val fragment: Fragment? =

            supportFragmentManager.findFragmentByTag(ReferralFragment:: class.java.simpleName)

            if(fragment !is ReferralFragment){
                supportFragmentManager.beginTransaction()
                    .add(R.id.mainContainer, myFragment, ReferralFragment:: class.java.simpleName)
                    .commit()
            }
        }
        else if(item.itemId == R.id.miEmojis)
        {
            val fragment = supportFragmentManager.findFragmentByTag(ReferralFragment:: class.java.simpleName)
            if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment)
                .commit()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialog() {
        val editText = EditText(this)
        // TODO: restrict input lenght and only to emojis

        val dialog = AlertDialog.Builder(this)
            .setTitle("Update your emojis")
            .setView(editText)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton((DialogInterface.BUTTON_POSITIVE)).setOnClickListener{
            Log.i(TAG, "Clicked on positive button!")
            val emojisEntered = editText.text.toString()

            if(emojisEntered.isBlank()){
                Toast.makeText(this, "Can not submit empty text", Toast.LENGTH_SHORT).show()
            }
            else{
                val currentUser = auth.currentUser
                if(currentUser == null){
                    Toast.makeText(this, "No signed in user", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                db.collection("users")
                    .document(currentUser.uid)
                    .update("emojis", emojisEntered)
                dialog.dismiss()
            }
        }
    }
}