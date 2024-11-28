package com.example.ble_iot_project

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.scan.BleScanRuleConfig
import com.example.ble_iot_project.MathTool.Circle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.kiba.coordinateaxischart.ChartConfig
import com.kiba.coordinateaxischart.CoordinateAxisChart
import com.kiba.coordinateaxischart.SinglePoint
import kotlin.math.max


//import kotlin.math.radian
private const val TRY_DISTANCE_STEP = 0.01
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_BLUETOOTH_SCAN = 123 // Any unique code


    private fun startBluetoothScan() {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with scanning
            // (Your code to scan for Bluetooth devices)
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_CODE_BLUETOOTH_SCAN)
        }
    }




    var a: ArrayList<Int?> = ArrayList<Int?>()

    // Allowed Bluetooth device MAC and number
    private val allowBluetoothDeviceMacs: HashMap<String, Int> = object : HashMap<String, Int>() {
        init {
            // Initialize the allowed Bluetooth devices
            put("08:B6:1F:3B:15:56", 0)
//        put("EC:F4:A0:9B:56:23", 0);  // For test
            put("08:D1:F9:92:17:02", 1)
            put("64:B7:08:4E:F0:02", 2)
        }
    }

    // Bluetooth device object
    private val bluetoothDevices: ArrayList<BleDevice?> = object : ArrayList<BleDevice?>() {
        init {
            add(null)
            add(null)
            add(null)
        }
    }

    private val bluetoothReadyStates = booleanArrayOf(false, false, false)



    private lateinit var bleCard1: CardView
    private lateinit var bleCard2: CardView
    private lateinit var bleCard3: CardView
    private lateinit var bleDevice1Rssi: TextView
    private lateinit var bleDevice2Rssi: TextView
    private lateinit var bleDevice3Rssi: TextView
    private lateinit var roomX: EditText
    private lateinit var roomY: EditText
    private lateinit var location: TextView
    private lateinit var coordinateAxisChart: CoordinateAxisChart
    private lateinit var fab: FloatingActionButton
    private lateinit var myConstraintLayout: ConstraintLayout
    private var snackbar: Snackbar? = null


    private fun calculate(){

        for (i in 0 until 2) {
            if (!bluetoothReadyStates[i]) {
                val number = i + 1
                snackbar?.dismiss()
                snackbar = Snackbar.make(myConstraintLayout, "Cannot get No.$number beacon's RSSI.\nGet location FAILED!", Snackbar.LENGTH_INDEFINITE)
                snackbar?.show()
                return
            }
        }

        val device1Point: MathTool.Point
        val device2Point: MathTool.Point
        val device3Point: MathTool.Point

        try {
            device1Point = MathTool.Point(0.0, 0.0)
            device2Point = MathTool.Point(roomX.text.toString().toDouble(), 0.0)
            device3Point = MathTool.Point(0.0, roomY.text.toString().toDouble())
        } catch (e: Exception) {
            if (snackbar != null) snackbar!!.dismiss()
            Snackbar.make(myConstraintLayout, "Invalid x or y range !", Snackbar.LENGTH_INDEFINITE)
                .show()
            return
        }

        // Convert all three rssi to actual distance

        // Convert all three rssi to actual distance
        val distances = DoubleArray(3)
        for (i in 0..2) {
            when (i) {
                else -> distances[i] =
                    MathTool.rssiToDistance(bluetoothDevices[i]!!.rssi.toDouble()) * Math.cos(
                        Math.toRadians(45.0)
                    )
            }
            Log.d("RSSI to distance : ", java.lang.Double.toString(distances[i]))
        }

        // Abstract circle

        // Abstract circle
        val circle1 = Circle(
            MathTool.Point(device1Point.x, device1Point.y),
            distances[0]
        )
        val circle2 = Circle(
            MathTool.Point(device2Point.x, device2Point.y),
            distances[1]
        )
        val circle3 = Circle(
            MathTool.Point(device3Point.x, device3Point.y),
            distances[2]
        )
        // Try to perform an operation
        // Try to perform an operation
        while (true) {
            // First look at whether there are intersections between the three circles.
            // If 1、2 no intersection between the two circles
            if (!MathTool.isTwoCircleIntersect(circle1, circle2)) {
                // Try increasing the radius of a circle，Who is bigger and who increases
                if (circle1.r > circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP
                } else {
                    circle2.r += TRY_DISTANCE_STEP
                }
                continue
            }
            // If there is no intersection between the two circles of 1, 3
            if (!MathTool.isTwoCircleIntersect(circle1, circle3)) {
                // Try increasing the radius
                // If the radius of c3 is smaller than either of them
                if (circle3.r < circle1.r && circle3.r < circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP
                    circle2.r += TRY_DISTANCE_STEP
                } else {
                    circle3.r += TRY_DISTANCE_STEP
                }
                continue
            }
            // If there is no intersection between the two originals
            if (!MathTool.isTwoCircleIntersect(circle2, circle3)) {
                // Try increasing the radius
                // If the radius of c3 is smaller than either of them
                if (circle3.r < circle1.r && circle3.r < circle2.r) {
                    circle1.r += TRY_DISTANCE_STEP
                    circle2.r += TRY_DISTANCE_STEP
                } else {
                    circle3.r += TRY_DISTANCE_STEP
                }
                continue
            }

            // When you try to find that the three circles have intersections, find the intersection between the two circles.
            val temp1 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle1, circle2)
            val temp2 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle2, circle3)
            val temp3 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle3, circle1)
            // The point where the intersection of the two circles of 1 and 2 takes y > 0
            val resultPoint1 = if (temp1.p1.y > 0) MathTool.Point(
                temp1.p1.x,
                temp1.p1.y
            ) else MathTool.Point(temp1.p2.x, temp1.p2.y)
            Log.d("resultPoint1", temp1.p1.toString() + "  " + temp1.p2.toString())
            // The intersection of 2, 3 and 2 circles takes the mean of the two
            val resultPoint2 = MathTool.Point(
                max(temp2.p1.x, temp2.p2.x),
                max(temp2.p1.y, temp2.p2.y)
            )
            // 3, 1 the intersection of the two circles takes the point where x > 0
            val resultPoint3 = if (temp3.p1.x > 0) MathTool.Point(
                temp3.p1.x,
                temp3.p1.y
            ) else MathTool.Point(temp3.p2.x, temp3.p2.y)

            // Find the center point of three points
            val resultPoint = MathTool.getCenterOfThreePoint(
                resultPoint1,
                resultPoint2,
                resultPoint3
            )
            Log.d("Location", "$resultPoint1  $resultPoint2  $resultPoint3")

            // Update result display
            if (snackbar != null) snackbar!!.dismiss()
            Snackbar.make(myConstraintLayout, "Get the location!", Snackbar.LENGTH_LONG).show()
            location.text = resultPoint.toString()
            val x_float = resultPoint.x.toFloat()
            val y_float = resultPoint.y.toFloat()
            val config = ChartConfig()


            // the max value of the axis Maximum value of the axis
//            config.setMax(10);
            try {
                config.max = max(roomX.text.toString().toInt(), roomY.text.toString().toInt())
            } catch (e: Exception) {
//                Toast.makeText(getApplicationContext(), "Invalid x or y range !", Toast.LENGTH_SHORT).show();
                if (snackbar != null) snackbar!!.dismiss()
                Snackbar.make(
                    myConstraintLayout,
                    "Invalid x or y range !",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
                return
            }
            config.precision = 1
            config.segmentSize = 50
            coordinateAxisChart.setConfig(config)
            coordinateAxisChart.reset()
            coordinateAxisChart.invalidate()
            val locPoint = SinglePoint(PointF(x_float, y_float))
            locPoint.pointColor = Color.RED
            coordinateAxisChart.addPoint(locPoint)
            val x_range_float: Float
            val y_range_float: Float
            try {
                x_range_float = roomX.text.toString().toInt().toFloat()
                y_range_float = roomY.text.toString().toInt().toFloat()
            } catch (e: Exception) {
                if (snackbar != null) snackbar!!.dismiss()
                Snackbar.make(myConstraintLayout, "Invalid x or y range !", Snackbar.LENGTH_LONG)
                    .show()
                return
            }
            val ble1Point = SinglePoint(PointF(0f, 0f))
            val ble2Point = SinglePoint(PointF(x_range_float, 0f))
            val ble3Point = SinglePoint(PointF(0f, y_range_float))
            ble1Point.pointColor = Color.GREEN
            ble2Point.pointColor = Color.GREEN
            ble3Point.pointColor = Color.GREEN
            coordinateAxisChart.addPoint(ble1Point)
            coordinateAxisChart.addPoint(ble2Point)
            coordinateAxisChart.addPoint(ble3Point)
            coordinateAxisChart.invalidate()
            break
        }
    }

    private fun scan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        bluetoothDevices.clear()
        for (i in 0..2) bluetoothDevices.add(null)
        // Configuring scan rules
        // Configuring scan rules
        BleManager.getInstance()
            .initScanRule(
                BleScanRuleConfig.Builder()
                    .setAutoConnect(false)
                    .setScanTimeOut(1000)
                    .build()
            )
        // Turn on Bluetooth
        // Turn on Bluetooth
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            // Bluetooth is not available or not enabled
            Log.i(TAG,"Bluetooth not enabled")
            // Handle accordingly
        } else {
            // Bluetooth is available and enabled
            // Proceed with scanning operations
            Log.i(TAG,"Bluetooth enabled")
        }

        BleManager.getInstance().enableBluetooth()

        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanFinished(scanResultList: List<BleDevice>) {
//                Toast.makeText(getApplicationContext(), "Finish Scanning!", Toast.LENGTH_SHORT)
//                        .show();
                fab.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.colorAccent))
                fab.isClickable = true
                calculate()
            }

            override fun onScanStarted(success: Boolean) {
                if (snackbar != null) snackbar!!.dismiss()
                Snackbar.make(myConstraintLayout, "Locating...", Snackbar.LENGTH_INDEFINITE).show()
                fab.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.disabled))
                fab.isClickable = false
            }

            override fun onScanning(bleDevice: BleDevice) {
                // If you have scanned a new device
                // See if he is pre-set by several devices
                for (mac in allowBluetoothDeviceMacs.keys) {
                    Log.d("BL", "get an bl device")
                    // if
                    if (mac == bleDevice.mac) {
                        // Get index
                        val index: Int? = allowBluetoothDeviceMacs.get(mac)
                        // Add the device to the list
                        bluetoothDevices.removeAt(index!!)
                        bluetoothDevices.add(index, bleDevice)

                        // Update display status
                        bluetoothReadyStates[index] = true
                        when (index) {
                            0 -> {
                                //                                bleDevice1Ready.setText("Ready");
                                bleCard1.setCardBackgroundColor(resources.getColor(R.color.success))
                                bleDevice1Rssi.text = bleDevice.rssi.toString()
                            }

                            1 -> {
                                //                                bleDevice2Ready.setText("Ready");
                                bleCard2.setCardBackgroundColor(resources.getColor(R.color.success))
                                bleDevice2Rssi.text = bleDevice.rssi.toString()
                            }

                            2 -> {
                                //                                bleDevice3Ready.setText("Ready");
                                bleCard3.setCardBackgroundColor(resources.getColor(R.color.success))
                                bleDevice3Rssi.text = bleDevice.rssi.toString()
                            }

                            else -> {}
                        }
                    }
                }
            }
        })

    }

    private fun bindComponent() {

        bleCard1 = findViewById<View>(R.id.bleCard1) as CardView
        bleCard2 = findViewById<View>(R.id.bleCard2) as CardView
        bleCard3 = findViewById<View>(R.id.bleCard3) as CardView
        bleDevice1Rssi = findViewById<TextView>(R.id.bleDevice1_rssi)
        bleDevice2Rssi = findViewById<TextView>(R.id.bleDevice2_rssi)
        bleDevice3Rssi = findViewById<TextView>(R.id.bleDevice3_rssi)
        roomX = findViewById<EditText>(R.id.room_x)
        roomY = findViewById<EditText>(R.id.room_y)
        location = findViewById<TextView>(R.id.location)
        coordinateAxisChart = findViewById<View>(R.id.coordinateAxisChart) as CoordinateAxisChart
        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        myConstraintLayout = findViewById<View>(R.id.myConstraintLayout) as ConstraintLayout
        fab.setOnClickListener {
            for (i in 0..1) {
                bluetoothReadyStates[i] = false
            }
            //                bleDevice1Ready.setText("Not ready");
            bleCard1.setCardBackgroundColor(resources.getColor(R.color.error))
            bleDevice1Rssi.text = "N/A"
            //                bleDevice2Ready.setText("Not ready");
            bleCard2.setCardBackgroundColor(resources.getColor(R.color.error))
            bleDevice2Rssi.text = "N/A"
            //                bleDevice3Ready.setText("Not ready");
            bleCard3.setCardBackgroundColor(resources.getColor(R.color.error))
            bleDevice3Rssi.text = "N/A"
            // Start a new round of scanning
            /* Declaring array of n elements, the value
                 * of n is provided by the user
                 */scan()
            // for automation
//                final ExecutorService es = Executors.newCachedThreadPool();
//                ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
//                ses.scheduleAtFixedRate(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        es.submit(new Runnable()
//                        {
//                            @Override
//                            public void run()
//                            {
//                                scan();
//                            }
//                        });
//
//                    }
//                }, 0, 9, TimeUnit.SECONDS);
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startBluetoothScan()
        BleManager.getInstance().init(getApplication())
        bindComponent()
    }


}