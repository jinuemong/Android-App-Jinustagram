package com.example.myapplication_instargram.followFragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication_instargram.Message.MessageRoomFragment
import com.example.myapplication_instargram.manager.FollowManager
import com.example.myapplication_instargram.server.MasterApplication
import com.example.myapplication_instargram.R
import com.example.myapplication_instargram.UpdateProfileActivity
import com.example.myapplication_instargram.UserActivity
import com.example.myapplication_instargram.manager.MessageManager
import com.example.myapplication_instargram.manager.MiniPosterAdapter
import com.example.myapplication_instargram.manager.PosterManager
import com.example.myapplication_instargram.unit.Profile
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private lateinit var masterApp: MasterApplication
private lateinit var username: String
private lateinit var followManager: FollowManager
private lateinit var messageManager: MessageManager
private lateinit var posterManager:PosterManager

class UserProfile(
    username : String
) : Fragment() {
    lateinit var userUiView: View
    lateinit var userNameView: TextView
    lateinit var userCustomNameView: TextView
    lateinit var userImageView: CircleImageView
    lateinit var userPostNum: TextView
    lateinit var userFollower: TextView
    lateinit var getUserFollower: LinearLayout
    lateinit var getUserFollowing: LinearLayout
    lateinit var userFollowing: TextView
    lateinit var userCommandView: TextView
    lateinit var followButton: Button
    lateinit var messageSendButton: Button
    lateinit var backButton: ImageView
    private lateinit var profilePosterRecycler: RecyclerView
    private  val targetUsername =username
    private lateinit var userActivity: UserActivity
    //?????? ?????? ?????? (???????????? ????????? )
    private lateinit var callback: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userActivity = context as UserActivity
        username = userActivity.username
        userActivity.targetUserName = targetUsername
        masterApp = userActivity.application as MasterApplication
        followManager = FollowManager(masterApp)
        messageManager = MessageManager(masterApp)
        posterManager = PosterManager(masterApp)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as UserActivity).onFragmentGoBack(this@UserProfile)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userUiView = inflater.inflate(R.layout.fragment_user_profile, container, false)
        return userUiView

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.startAnimation(userActivity.viewChangeAnimateLeft)
        //??? ?????????
        initView(userUiView)

        //????????? ??????
        setUpListener()
    }

    override fun onResume() {
        super.onResume()
        loadUI() //?????? ?????? ??? ?????????

    }

    //??? ??????
    private fun initView(UserUiView: View) {
        userNameView = UserUiView.findViewById(R.id.frag_profile_user_id)
        userCustomNameView = UserUiView.findViewById(R.id.profile_custom_name)
        userImageView = UserUiView.findViewById(R.id.frag_profile_user_image)
        userPostNum = UserUiView.findViewById(R.id.frag_profile_user_post_num)
        getUserFollower = UserUiView.findViewById(R.id.Lin_frag_profile_user_follower)
        getUserFollowing = UserUiView.findViewById(R.id.Lin_frag_profile_user_following)
        userFollower = UserUiView.findViewById(R.id.frag_profile_user_follower_num)
        userFollowing = UserUiView.findViewById(R.id.frag_profile_user_following_num)
        userCommandView = UserUiView.findViewById(R.id.frag_profile_user_command)
        followButton = UserUiView.findViewById(R.id.frag_profile_user_follow_button)
        messageSendButton = UserUiView.findViewById(R.id.frag_profile_user_message_button)
        profilePosterRecycler = UserUiView.findViewById(R.id.frag_profile_user_recycler)
        backButton = UserUiView.findViewById(R.id.frag_profile_user_back_button)
        loadUI()

    }

    private fun loadUI() {
        //?????? UI??? ?????????
        masterApp.service.getUserProfile(username = targetUsername)
            .enqueue(object : Callback<ArrayList<Profile>> {
                override fun onResponse(
                    call: Call<ArrayList<Profile>>,
                    response: Response<ArrayList<Profile>>
                ) {
                    if (response.isSuccessful) {
                        try {
                            val profile = response.body()!![0]
                            userNameView.text = profile.username
                            userCustomNameView.text = profile.customName
                            userCommandView.text = profile.userComment
                            profile.userImage?.let { userActivity.setCircleImage(it,userImageView) }
                            userPostNum.text = profile.posterCount.toString()
                            userFollower.text = profile.followerCount.toString()
                            userFollowing.text = profile.followingCount.toString()
                            userCommandView.text = profile.userComment
                            //?????? ???????????? ????????? ??????
                            if (username == targetUsername) {
                                messageSendButton.text = "????????? ??????"
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                override fun onFailure(call: Call<ArrayList<Profile>>, t: Throwable) {}
            })
        posterManager.getPosterList(targetUsername, paramFunc = { posterList->
            if(posterList!=null) {
                val adapter = MiniPosterAdapter(userActivity,
                    posterList, LayoutInflater.from(userActivity))
                profilePosterRecycler.adapter = adapter
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setUpListener() {
        //????????? ?????? ??????
        messageSendButton.setOnClickListener {
            //?????? ???????????? ????????? ??????
            if (username == targetUsername) {
                val upIntent = Intent(userActivity, UpdateProfileActivity::class.java)
                upIntent.putExtra("username", "" + username)
                startActivity(upIntent)
            } else {
                messageManager.isExistMessageRoom(username,
                    targetUsername, paramFunc = { responseExist->
                    if(responseExist!=-2){
                        //-2??? ????????? ?????? ?????? , -1??? ?????? messageRoom ??????
                        if(responseExist==-1){
                            //???????????? ??????
                            messageManager.createMessageRoom(username,
                                targetUsername, paramFunc = { responseCreate->
                                if(responseCreate>=0){ //-1,-2,-3??? ???????????? ???????????? ??????+??????
                                    messageManager.getMessageList(responseCreate, paramFunc = {
                                        userActivity.onFragmentChange(MessageRoomFragment(responseCreate,it,0))
                                    })
                                }
                            })
                        }else{
                            messageManager.getMessageList(responseExist, paramFunc = { messagePostList->
                                var messagePosition=0
                                if (messagePostList!=null){
                                    val messageUserList = messagePostList.messageUserPost
                                    val messageList = messagePostList.messagePost
                                    var lastReadMessage = 0
                                    if (messageUserList != null) {
                                        for (messageUser in messageUserList){
                                            if (messageUser.username== userActivity.username){
                                                lastReadMessage = messageUser.lastReadMessageId
                                            }
                                            if(!messageUser.isActive){
                                                messageManager.patchActive(messageUser.id)
                                            }
                                        }
                                    }
                                    if (messageList!=null){
                                        var count =0
                                        for(message in messageList){
                                            if (message.messageId==lastReadMessage){
                                                messagePosition = count
                                            }else{
                                                count+=1
                                            }
                                        }
                                    }

                                }
                                userActivity.onFragmentChange( MessageRoomFragment(responseExist,messagePostList,messagePosition))
                            })
                        }
                    }
                })
            }
        }
        //??????????????? ??????+ ?????? ??? ????????? + ?????????
        followManager.setButtonsListener(2, followButton, username, targetUsername, paramFunc = {
            if (it) {
                userFollower.text = ((userFollower.text).toString().toInt() + 1).toString()
            } else {
                userFollower.text = ((userFollower.text).toString().toInt() - 1).toString()
            }
        })

        //????????? ??????
        getUserFollower.setOnClickListener {
            userActivity.onFragmentChange(
                FollowMainFragment(0,targetUsername)
            )
        }
        //????????? ??????
        getUserFollowing.setOnClickListener {
            userActivity.onFragmentChange(
                FollowMainFragment(1,targetUsername)
            )
        }

        //???????????? ?????? ??????
        backButton.setOnClickListener {
            userActivity.onFragmentGoBack(this@UserProfile)
        }
    }
}

//class RecyclerViewAdapter(
//    val itemList : ArrayList<>,
//    val inflater: LayoutInflater
//) : RecyclerView.Adapter<RecyclerViewAdapter.>(){
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ??? {
//        TODO("Not yet implemented")
//    }
//
//    override fun onBindViewHolder(holder: ???, position: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getItemCount(): Int {
//        TODO("Not yet implemented")
//    }
//
//}