package org.poseestimation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.poseestimation.socketconnect.bluetoothReceiver.BluetoothMesgReceiver
import org.poseestimation.socketconnect.connectview.BluetoothWearviewActivity
import org.poseestimation.socketconnect.connectview.hostviewActivity
import org.poseestimation.socketconnect.connectview.slaveviewActivity
import org.poseestimation.socketconnect.screenprojection.screen_sender_connectView

class testActivity : AppCompatActivity() {
    lateinit var btn1:Button
    lateinit var btn2:Button
    lateinit var btn3:Button
    lateinit var btn4:Button
    lateinit var btn5:Button
    lateinit var btn6:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        btn1=findViewById(R.id.Btn1)
        btn2=findViewById(R.id.Btn2)
        btn3=findViewById(R.id.Btn3)
        btn4=findViewById(R.id.Btn4)
        btn5=findViewById(R.id.Btn5)
        btn6=findViewById(R.id.Btn6)
        btn1.setOnClickListener {
            val intent = Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
        }
        btn2.setOnClickListener {
            val intent = Intent(baseContext, hostviewActivity::class.java)
            startActivity(intent)
        }
        btn3.setOnClickListener {
            val intent = Intent(baseContext, slaveviewActivity::class.java)
            startActivity(intent)
        }
        btn4.setOnClickListener {
            val intent = Intent(baseContext, screen_sender_connectView::class.java)
            startActivity(intent)
        }
        btn5.setOnClickListener {
            val intent = Intent(baseContext, slaveviewActivity::class.java)
            startActivity(intent)
        }
        btn6.setOnClickListener {
            val intent = Intent(baseContext, BluetoothWearviewActivity::class.java)
            startActivity(intent)
        }

    }
}