package hu.isti115.tellopad

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Math.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
// import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
  lateinit var textView: TextView
  private lateinit var socket: DatagramSocket

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val policy = ThreadPolicy.Builder().permitAll().build()
    StrictMode.setThreadPolicy(policy)

    textView = findViewById(R.id.tmp) as TextView
    textView.text = "Tello Gamepad Remote by Isti115\n---\n"
    // textView.setOnClickListener {
    //   if (textView.text == "sajt") {
    //     textView.text = "Tello Gamepad Remote by Isti115"
    //   } else {
    //     textView.text = "sajt"
    //   }
    // }

    socket = DatagramSocket(8890)

    val uiHandler = Handler(Looper.getMainLooper())
    val receiveThread = Thread(Receiver(socket, textView, uiHandler))
    receiveThread.start()

    send("command")
  }

  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
    if (event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK)) {
      // textView.text = "" + event.action
      if (event.action == MotionEvent.ACTION_MOVE) {
        textView.text = "" + event.getAxisValue(MotionEvent.AXIS_X) + "\n---\n"
        // textView.text = "" + event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        // process the joystick movement...
        // textView.text = "ghj" + event.getAxisValue(MotionEvent.AXIS_HAT_X)

        val LT = event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 1f
        val RT = event.getAxisValue(MotionEvent.AXIS_RTRIGGER) == 1f
        val left = event.getAxisValue(MotionEvent.AXIS_HAT_X) == -1f
        val right = event.getAxisValue(MotionEvent.AXIS_HAT_X) == 1f
        val up = event.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1f
        val down = event.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1f
        if (LT && up) { send("takeoff") } else
        if (LT && down) { send("land") } else
        if (LT && RT) { send("emergency") } else
        {
          val yaw = normalize(event.getAxisValue(MotionEvent.AXIS_X) * 100)
          val throttle = -normalize(event.getAxisValue(MotionEvent.AXIS_Y) * 100)
          val roll = normalize(event.getAxisValue(MotionEvent.AXIS_Z) * 100)
          val pitch = -normalize(event.getAxisValue(MotionEvent.AXIS_RZ) * 100)

          send("rc $roll $pitch $throttle $yaw")
          // textView.text = "ghj" + event.getAxisValue(MotionEvent.AXIS_Z)
        }
        return true
      }

      // else if (event.action == MotionEvent.ACTION_BUTTON_PRESS)
      // send(""+event.getActionButton())
    }

    return super.onGenericMotionEvent(event)
  }

  fun normalize(value: Float): Int {
    return if (abs(round(value)) < 5) 0 else round(value)
  }

  fun send(str: String) {
    val bytes = str.toByteArray()
    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("192.168.10.1"), 8889))
  }
}

class Receiver(val socket: DatagramSocket, val output: TextView, val uiHandler: Handler): Runnable {
  public override fun run() {
    var i = 1
    while (true) {
      // Thread.sleep(1000)
      // delay(1000L)

      val buffer = ByteArray(1024)
      val packet = DatagramPacket(buffer, buffer.size)
      socket.receive(packet)

      uiHandler.post {
        output.text = output.text.replace(
          Regex("---.*", RegexOption.DOT_MATCHES_ALL),
          "---\n${i}\n" + String(packet.data).replace(';', '\n')
        )
      }

      // uiHandler.post(object : Runnable {
      //   override fun run() {
      //     output.text = "${Thread.currentThread()} Runnable Thread Started. ${i}"
      //   }
      // })
      i++
    }
  }
}
