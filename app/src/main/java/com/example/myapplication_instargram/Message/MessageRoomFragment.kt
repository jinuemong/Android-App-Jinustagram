package com.example.myapplication_instargram.Message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.KeyEvent.KEYCODE_ENTER
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication_instargram.R
import com.example.myapplication_instargram.SelectPicActivity
import com.example.myapplication_instargram.UserActivity
import com.example.myapplication_instargram.ViewModel
import com.example.myapplication_instargram.manager.MessageManager
import com.example.myapplication_instargram.server.MasterApplication
import com.example.myapplication_instargram.unit.Message
import com.example.myapplication_instargram.unit.MessageDataMultiMode
import com.example.myapplication_instargram.unit.MessageDataMultiMode.Companion.item_image_message_my
import com.example.myapplication_instargram.unit.MessageDataMultiMode.Companion.item_image_message_other
import com.example.myapplication_instargram.unit.MessageDataMultiMode.Companion.item_text_message_my
import com.example.myapplication_instargram.unit.MessageDataMultiMode.Companion.item_text_message_other
import com.example.myapplication_instargram.unit.MessageList
import com.example.myapplication_instargram.unit.MiniProfiles
import com.example.myapplication_instargram.userServeFragment.StoryFragment
import de.hdodenhof.circleimageview.CircleImageView

private lateinit var masterApp: MasterApplication
private lateinit var messageManager: MessageManager
private lateinit var username: String
private lateinit var chattingList: RecyclerView
private lateinit var userActivity: UserActivity
private lateinit var targetUserName:String
class MessageRoomFragment(messageId: Int, messageList: MessageList?,messagePosition:Int) : Fragment() {
    private var multiList = ArrayList<MessageDataMultiMode>() //????????? ?????????
    private val messageList = messageList?.messagePost //????????? ?????????
    private val messageUserList = messageList?.messageUserPost
    private val messageRoomIdFromFragment = messageId
    private var messageNum = messagePosition
    private lateinit var adapter: MessageListViewAdapter
    lateinit var messageRoomView: View
    private lateinit var goBackButton: ImageView
    private lateinit var targetUserImage: CircleImageView
    private lateinit var targetUsername: TextView
    private lateinit var targetUserCustomName: TextView
    private lateinit var menuButton: ImageView
    private lateinit var typingMessage: EditText
    private lateinit var sendMessageButton: ImageView
    private lateinit var loadGalleryButton: ImageView

    //?????? ?????? ?????? (???????????? ????????? )
    private lateinit var callback: OnBackPressedCallback
    private lateinit var fm: FragmentManager

    //?????? ????????????
    private var selectedImageUrl: String? = "" //???????????? ????????? ???????????? ??????
    private lateinit var getTakePicture: ActivityResultLauncher<Intent>
    private lateinit var viewModel: ViewModel
    override fun onAttach(context: Context) {
        super.onAttach(context)
        userActivity = context as UserActivity
        username = (activity as UserActivity).username
        targetUserName =(activity as UserActivity).targetUserName
        fm = (activity as UserActivity).supportFragmentManager //??????????????? ?????????
        masterApp = (activity as UserActivity).application as MasterApplication
        messageManager = MessageManager(masterApp)
        viewModel = ViewModel()
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                userActivity.onFragmentGoBack(MessageRoomListFragment())
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onDetach() {
        super.onDetach()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        messageRoomView = inflater.inflate(R.layout.fragment_message_room, container, false)
        return messageRoomView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.startAnimation(userActivity.viewChangeAnimateLeft)
        initView(messageRoomView)
        //?????? ?????? ??? uri ????????????
        getTakePicture = registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val intent = it.data
                selectedImageUrl = intent?.getStringExtra("selectedImageUrl")
                //selectedImageUrl - > ????????? url ??? ??????????????? ????????? !
                viewModel.sendImageMessage(
                    messageRoomIdFromFragment,
                    username,
                    Uri.parse(selectedImageUrl),
                    userActivity,
                    paramFunc = { message ->
                        if (message != null) {
                            multiList.add(multiDataChange(message))
                            adapter.notifyItemInserted(adapter.itemCount - 1)
                            chattingList.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    })
            }
        }
        //????????? ?????????
        setUpListener()

        //????????? ?????? ?????????
        for (message in messageList!!) {
            if (message.messageRoomId != -1) {
                //???????????? ???????????? ???
                multiList.add(multiDataChange(message))
            }
        }

        //????????? ??????
        adapter = MessageListViewAdapter(multiList, LayoutInflater.from(userActivity))
        chattingList.adapter = adapter
        chattingList.layoutManager = LinearLayoutManager(userActivity)
        try {
            chattingList.scrollToPosition(messageNum)
        }catch (e:Exception){
            chattingList.scrollToPosition(multiList.size-1)
        }
        //????????? ?????????
        messageListener()


    }

    private fun initView(view: View) {
        goBackButton = view.findViewById(R.id.message_room_back_button)
        targetUserImage = view.findViewById(R.id.message_room_user_image)
        targetUsername = view.findViewById(R.id.message_room_username)
        targetUserCustomName = view.findViewById(R.id.message_room_custom_name)
        menuButton = view.findViewById(R.id.message_room_user_menu_button)
        chattingList = view.findViewById(R.id.message_room_recycler)
        typingMessage = view.findViewById(R.id.message_room_message_ty)
        sendMessageButton = view.findViewById(R.id.message_room_send_message)
        loadGalleryButton = view.findViewById(R.id.message_room_gallery)
        masterApp.service.getUserProfileMini(targetUserName)
            .enqueue(object : Callback<ArrayList<MiniProfiles>> {
                override fun onResponse(
                    call: Call<ArrayList<MiniProfiles>>,
                    response: Response<ArrayList<MiniProfiles>>
                ) {
                    if (response.isSuccessful) {
                        try {
                            if (response.body()!!.size > 0) {
                                response.body()!![0].userImage?.let { userActivity.setCircleImage(it,targetUserImage) }

                                if (response.body()!![0].storyCount > 0) {
                                    targetUserImage.apply {
                                        //????????? ??????
                                        borderColor = Color.parseColor("#62BEF3")
                                        // ????????? ??????
                                        borderWidth *= 2
                                    }
                                    //????????? ????????? ?????? ?????????
                                    targetUserImage.setOnClickListener {
                                        userActivity.onFragmentChange(
                                            StoryFragment(
                                                response.body()!![0].storyPost!!,
                                                response.body()!![0].username,
                                                response.body()!![0].userImage
                                            )
                                        )
                                    }
                                }
                                targetUsername.text = response.body()!![0].username
                                targetUserCustomName.text = response.body()!![0].customName
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                override fun onFailure(call: Call<ArrayList<MiniProfiles>>, t: Throwable) {
                }

            })
    }

    private fun setUpListener() {
        goBackButton.setOnClickListener {
            userActivity.onFragmentGoBack(MessageRoomListFragment())
        }
        //????????? ???????????? or ?????? ??????
        loadGalleryButton.setOnClickListener {
            val imageIntent = Intent(userActivity.applicationContext, SelectPicActivity::class.java)
            imageIntent.putExtra("username", username)
            imageIntent.putExtra("roomId", messageRoomIdFromFragment)
            getTakePicture.launch(imageIntent)
        }
    }

    private fun messageListener() {
        sendMessageButton.setOnClickListener {
            sendTextMessage()
        }

        //?????? ??? ?????? , ??? ????????????
       typingMessage.setOnKeyListener { v,keyCode ,event ->
            var handled = false
            if(event.action==KeyEvent.ACTION_DOWN && keyCode == KEYCODE_ENTER){
                sendTextMessage()
                handled = true
            }
            handled
        }
        //????????? ????????? ?????? ??????????????? ??? ?????????
        chattingList.addOnItemTouchListener(object :
            RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y)
                if (child != null) {
                    val position = rv.getChildAdapterPosition(child)
                    val view = rv.layoutManager?.findViewByPosition(position)
                    val id =
                        view?.findViewById<TextView>(R.id.message_id_my)?.text.toString().toInt()
                    val itemDelButton = view?.findViewById<ImageView>(R.id.my_message_delete)
                    itemDelButton?.setOnClickListener {
                        //????????? ?????? ??????
                        messageManager.delMessage(id, paramFunc = {
                            if (it != -1) {
                                multiList.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                adapter.notifyItemRangeChanged(
                                    position,
                                    rv.adapter!!.itemCount - position
                                )
                            }
                        })
                    }
                }
                return false
            }
            //To change body of created functions use File | Settings | File Templates.
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            //To change body of created functions use File | Settings | File Templates.
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }
    private fun sendTextMessage() {
        messageManager.sendTextMessage(messageRoomIdFromFragment, username,
            typingMessage.text.toString(), paramFunc = { message ->
                if (message != null) {
                    if (message.messageRoomId != -1) {
                        //???????????? ???????????? ???
                        typingMessage.setText("")
                        multiList.add(multiDataChange(message))
                        adapter.notifyItemInserted(adapter.itemCount - 1)
                        chattingList.smoothScrollToPosition(adapter.itemCount - 1)
                    }
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //??? ?????? ??? lastReadMessageId  ??????
        if (messageUserList!=null){
            for (messageUser in messageUserList){
                if (messageUser.username== username){
                    messageManager.patchLastReadMessageId(messageUser.id, multiList.last().id)
                }
            }
        }
    }
}


//????????? ???????????????, ????????? ???????????????
// ?????? ?????????, ??? ????????? ??????           ???????????? ?????? ????????????
// username =  writer ??? ?????????
class MessageListViewAdapter(
    private val itemList: ArrayList<MessageDataMultiMode>,
    private val inflater: LayoutInflater,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return itemList[position].type
    }

    //1?????? ????????? 2?????? ?????????
    inner class MyViewHolder(itemView: View, ty: Int) : RecyclerView.ViewHolder(itemView) {
        val itemDelButton: ImageView
        val itemDay: TextView
        val itemTime: TextView
        val itemText: TextView
        val itemImage: ImageView
        val itemId: TextView

        init {
            itemDelButton = itemView.findViewById(R.id.my_message_delete)
            itemDay = itemView.findViewById(R.id.my_message_day)
            itemTime = itemView.findViewById(R.id.my_message_time)
            itemText = itemView.findViewById(R.id.my_message_text)
            itemImage = itemView.findViewById(R.id.my_message_image)
            itemId = itemView.findViewById(R.id.message_id_my)

            //????????? ???-> ????????? ????????? ????????? visible
            if (ty == 2) {
                itemText.visibility = View.INVISIBLE
                itemImage.visibility = View.VISIBLE
            }
        }
    }

    inner class OtherViewHolder(itemView: View, ty: Int) : RecyclerView.ViewHolder(itemView) {
        val itemUserImage: CircleImageView
        val itemDay: TextView
        val itemTime: TextView
        val itemText: TextView
        val itemImage: ImageView
        val itemId: TextView

        init {
            itemUserImage = itemView.findViewById(R.id.other_message_profile_image)
            masterApp.service.getUserProfileMini(targetUserName)
                .enqueue(object : Callback<ArrayList<MiniProfiles>> {
                    override fun onResponse(
                        call: Call<ArrayList<MiniProfiles>>,
                        response: Response<ArrayList<MiniProfiles>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()!![0].userImage?.let {
                                userActivity.setCircleImage(it,itemUserImage)
                            }
                        }
                    }

                    override fun onFailure(call: Call<ArrayList<MiniProfiles>>, t: Throwable) {}

                })
            itemDay = itemView.findViewById(R.id.other_message_day)
            itemTime = itemView.findViewById(R.id.other_message_time)
            itemText = itemView.findViewById(R.id.other_message_text)
            itemImage = itemView.findViewById(R.id.other_message_image)
            itemId = itemView.findViewById(R.id.message_id_other)

            //????????? ???-> ????????? ????????? ????????? visible
            if (ty == 2) {
                itemText.visibility = View.INVISIBLE
                itemImage.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            item_text_message_my -> { //??? ????????? ?????????
                view = inflater.inflate(R.layout.item_my_message, parent, false)
                MyViewHolder(view, 1)
            }
            item_image_message_my -> { //??? ????????? ?????????
                view = inflater.inflate(R.layout.item_my_message, parent, false)
                MyViewHolder(view, 2)
            }
            item_text_message_other -> { //?????? ????????? ?????????
                view = inflater.inflate(R.layout.item_other_message, parent, false)
                OtherViewHolder(view, 1)
            }
            item_image_message_other -> { //?????? ????????? ?????????
                view = inflater.inflate(R.layout.item_other_message, parent, false)
                OtherViewHolder(view, 2)
            }
            else -> throw RuntimeException("??? ??? ?????? ??? ?????? ")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uploadTimeStamp = itemList[position].uploadTime.split("-", "T", ":", ".")
        //22-09-12 ???????????? ??????
        val day =
            (uploadTimeStamp[0].chunked(2)[1] + "." + uploadTimeStamp[1] + "." + uploadTimeStamp[2])
        //??? : ??? ????????? ??????
        val time = uploadTimeStamp[3] + ":" + uploadTimeStamp[4]
        when (itemList[position].type) {
            item_text_message_my -> {
                val viewHolder = (holder as MyViewHolder)
//                viewHolder.itemDelButton.setOnClickListener {
//                    //????????? ?????? ??????
//                    messageManager.delMessage(itemList[position].id, paramFunc = {
//                        if (it!=-1){
//                            itemList.removeAt(position)
//                            super.notifyItemRemoved(position)
//                            super.notifyItemRangeChanged(position, itemCount - position)
//                        }
//                    })
//                }
                viewHolder.itemId.text = itemList[position].id.toString()
                viewHolder.itemText.text = itemList[position].body

                viewHolder.itemDay.text = day
                viewHolder.itemTime.text = time
            }
            item_image_message_my -> {
                val viewHolder = (holder as MyViewHolder)
//                viewHolder.itemDelButton.setOnClickListener {
//                    //????????? ?????? ??????
//                    messageManager.delMessage(itemList[position].id, paramFunc = {
//                        if (it!=-1){
//                            itemList.removeAt(position)
//                            super.notifyItemRemoved(position)
//                            super.notifyItemRangeChanged(position, itemCount - position)
//                        }
//                    })
//                }
                viewHolder.itemId.text = itemList[position].id.toString()
                userActivity.setRectImage(itemList[position].messageImage,viewHolder.itemImage)

                viewHolder.itemDay.text = day
                viewHolder.itemTime.text = time

            }
            item_text_message_other -> {
                val viewHolder = (holder as OtherViewHolder)
                viewHolder.itemId.text = itemList[position].id.toString()
                viewHolder.itemText.text = itemList[position].body

                viewHolder.itemDay.text = day
                viewHolder.itemTime.text = time
            }
            item_image_message_other -> {
                val viewHolder = (holder as OtherViewHolder)
                viewHolder.itemId.text = itemList[position].id.toString()
                userActivity.setRectImage(
                    itemList[position].messageImage,viewHolder.itemImage)

                viewHolder.itemDay.text = day
                viewHolder.itemTime.text = time
            }
            else -> {}
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}

private fun multiDataChange(message: Message): MessageDataMultiMode {
    val returnMessageMultiData: MessageDataMultiMode
    //?????? ??????
    var type: Int = if (message.messageImage == null) {
        if (message.writer == username) {
            1
        } else {
            2
        }
    } else {  // 2. ????????? ?????????
        if (message.writer == username) {
            3
        } else {
            4
        }
    }
    //????????? ?????????
    val body = if (message.body == null) {
        ""
    } else {
        message.body.toString()
    }

    //????????? ?????????
    val image = if (message.messageImage == null) {
        ""
    } else {
        message.messageImage.toString()
    }
    when (type) {
        1 -> { //??? ????????? message
            returnMessageMultiData = MessageDataMultiMode(
                item_text_message_my,
                message.messageId,
                message.writer,
                message.uploadTime,
                body,
                ""
            )
        }
        2 -> { //?????? ????????? message
            returnMessageMultiData = MessageDataMultiMode(
                item_text_message_other,
                message.messageId,
                message.writer,
                message.uploadTime,
                body,
                ""
            )
        }
        3 -> { // ???  ?????????  message
            returnMessageMultiData = MessageDataMultiMode(
                item_image_message_my,
                message.messageId,
                message.writer,
                message.uploadTime,
                "",
                image
            )
        }
        4 -> { //?????? ????????? message
            returnMessageMultiData = MessageDataMultiMode(
                item_image_message_other,
                message.messageId,
                message.writer,
                message.uploadTime,
                "",
                image
            )
        }
        //????????? ?????? -1 ??????
        else -> {
            returnMessageMultiData = MessageDataMultiMode(
                item_text_message_my,
                -1, "", "", "", ""
            )
        }
    }
    return returnMessageMultiData
}