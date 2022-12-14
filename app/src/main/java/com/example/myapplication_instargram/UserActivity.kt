package com.example.myapplication_instargram

import android.annotation.SuppressLint
import android.content.Context

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapplication_instargram.server.MasterApplication
import com.example.myapplication_instargram.userFragment.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UserActivity : AppCompatActivity() {
    lateinit var frManager: FragmentManager
    lateinit var username :String
    var targetUserName  = ""
    lateinit var viewClickAnimate: Animation
    lateinit var viewChangeAnimateUp:Animation
    lateinit var viewChangeAnimateLeft:Animation
    lateinit var pagerAdapter: FragmentAdapter
    lateinit var viewPager : ViewPager2
    private lateinit var tabUser : TabLayout
    lateinit var masterApp : MasterApplication
    lateinit var baseUrl :String
//    var fragmentStack  = ArrayList<Fragment>()
    var type: Int = 0
    private val tintColor= ColorStateList(
    arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf(-android.R.attr.state_selected)),
        //?????? ????????? ????????? ??????~ ??????, ????????? - ????????? ?????? ????????? ??? ?????? ???
    intArrayOf(Color.parseColor("#FF000000"),Color.parseColor("#FF6B99B3"))
    )

    //????????? ??? ????????? ??????
    private val iconView = arrayOf(
        R.drawable.ic_baseline_home_24,
        R.drawable.ic_baseline_search_24,
        R.drawable.ic_baseline_person_search_24,
        R.drawable.ic_baseline_videocam_24,
        R.drawable.ic_baseline_face_24,
    )
    override fun onResume() {
        super.onResume()
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        username = intent.getStringExtra("username").toString()
        masterApp = application as MasterApplication
        baseUrl = masterApp.baseUrl
        frManager = this@UserActivity.supportFragmentManager
        val isType  = intent.getStringExtra("type")
        if(isType!=null){ type=isType.toInt() }
        //??????????????? ?????????
        viewClickAnimate =  AnimationUtils.loadAnimation(this@UserActivity,R.anim.wave)
        viewChangeAnimateUp =  AnimationUtils.loadAnimation(this@UserActivity,R.anim.enter_up)
        viewChangeAnimateLeft =  AnimationUtils.loadAnimation(this@UserActivity,R.anim.enter_left)
        //????????? ?????????
        pagerAdapter = FragmentAdapter(this@UserActivity)
        pagerAdapter.addFragment(Home())
        pagerAdapter.addFragment(Search())
        pagerAdapter.addFragment(Explore())
        pagerAdapter.addFragment(FragmentVideo())
        pagerAdapter.addFragment(FragmentUserUI())
        viewPager = findViewById(R.id.view_user_view_pager)
        tabUser = findViewById(R.id.tab_user)
        viewPager.adapter = pagerAdapter

        //????????? ?????? ??? ?????? ??????????????? ??????
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                viewPager.isUserInputEnabled = true
                super.onPageSelected(position)
            }
        })
        if (type!=0){
            viewPager.currentItem = type
            type=0
        }
        tabUser.tabIconTint = tintColor
        //tabLayout attach
        TabLayoutMediator(tabUser,viewPager){ tab, position ->
            tab.setIcon(iconView[position])
        }.attach()
    }
    fun onTestChange(fromFragment: Fragment,goFragment: Fragment){
        frManager.beginTransaction().add(R.id.view_container,goFragment)
            .addToBackStack(null)
            .commit()
        frManager.beginTransaction().hide(fromFragment).commit()
        frManager.beginTransaction().show(goFragment).commit()
    }
    fun onFragmentChange(goFragment: Fragment){
        frManager.beginTransaction().replace(R.id.view_container,goFragment)
            .addToBackStack(null)
            .commit()
    }
    fun onFragmentGoBack(fragment: Fragment){
        frManager.beginTransaction().remove(fragment).commit()
        frManager.popBackStack()

    }

    @SuppressLint("SimpleDateFormat")
    fun getTranslatedDate(uploadTime : String) : String{
        //?????? ????????? ?????? ??????
        //?????? ????????? ???????????? ????????????
        val uploadTimeStamp = uploadTime.split("-", "T", ":", ".")
        //("yyyy-MM-dd HH:mm:ss")
        val time =uploadTimeStamp[0] + "-" +uploadTimeStamp[1]+"-"+uploadTimeStamp[2]+
                " "+uploadTimeStamp[3]+":"+uploadTimeStamp[4]+":"+uploadTimeStamp[5].chunked(2)[0]
        val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val data = sf.parse(time)
        val today = Calendar.getInstance()
        val calDate = (today.time.time-data!!.time) //??????, ?????? ??????
        return if ((calDate/(60*60*24*1000))>=1){ //1??? ??????
            (calDate/(60*60*24*1000)).toInt().toString()+" ??? ???"
        }else if ((calDate/(60*60*1000))>=1){ //1?????? ??????
            (calDate/(60*60*1000)).toInt().toString() + " ?????? ???"
        }else if((calDate/(60*1000))>=1){ //??? ??????
            (calDate/(60*1000)).toInt().toString() + " ??? ???"
        }else{
            "?????? ???"
        }
    }

    fun setRectImage(imageString : String,view:ImageView){
        try {
            if (imageString.contains(":")) {
                Glide.with(this@UserActivity)
                    .load(Uri.parse(imageString))
                    .override(view.width,view.height)
                    .into(view)
            } else {
                Glide.with(this@UserActivity)
                    .load(Uri.parse(baseUrl + imageString))
                    .override(view.width,view.height)
                    .into(view)
            }
        }catch (e:Exception){}
    }
    fun setCircleImage(imageString : String,view:CircleImageView){
        try {
            if (imageString.contains(":")) {
                Glide.with(this@UserActivity)
                    .load(Uri.parse(imageString))
                    .override(view.width,view.height)
                    .into(view)
            } else {
                Glide.with(this@UserActivity)
                    .load(Uri.parse(baseUrl + imageString))
                    .override(view.width,view.height)
                    .into(view)
            }
        }catch (e:Exception){}
    }
}

//class AddPosterAdapterDecoration(context : Context) : RecyclerView.ItemDecoration(){
//    //????????? ?????? ????????? ????????? ???
//    val userActivity =context
//    override fun getItemOffsets(
//        outRect: Rect,
//        view: View,
//        parent: RecyclerView,
//        state: RecyclerView.State
//    ) {
//        super.getItemOffsets(outRect, view, parent, state)
//    }
//
//}

//view??? ????????? ????????? ????????? ???????????? ??????
fun setAutoSizeView(view:View){
    //??????????????? ????????? view??? w,h??? ??????
    val vto = view.viewTreeObserver
    vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            val w = view.measuredWidth
            view.layoutParams = FrameLayout.LayoutParams(w, w)
        }
    })
}