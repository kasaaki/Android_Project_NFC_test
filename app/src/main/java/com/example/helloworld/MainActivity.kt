package com.example.helloworld

import android.app.PendingIntent
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.*
import android.util.Log
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    // ボタン、テキスト変数宣言
    private  var textMessage_read: TextView? = null
    private  var textMessage_write: TextView? = null
    private  var togglebutton_read: CompoundButton? = null
    private  var togglebutton_write: CompoundButton? = null

    // フラグ用変数
    private  var NFCmodeflag: Int? = 0 // INT型

    // 書き込むメッセージ
    val text = "write-test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // 受け取るIntentを指定
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        // 反応するタグの種類を指定、規格例はあくまでも一例
        techLists = arrayOf(
                arrayOf(android.nfc.tech.TagTechnology::class.java.name),   // すべてのタグ テクノロジー クラスで実装する必要があるインターフェース
                arrayOf(android.nfc.tech.IsoDep::class.java.name),          // 規格例：運転免許証 ISO-DEP（ISO 14443-4）
                arrayOf(android.nfc.tech.MifareClassic::class.java.name),   // 規格例：社員証
                arrayOf(android.nfc.tech.NdefFormatable::class.java.name),  // 規格例：社員証
                arrayOf(android.nfc.tech.NfcA::class.java.name),            // 規格例：社員証 NFC-A（ISO 14443-3A）
                arrayOf(android.nfc.tech.NfcB::class.java.name),            // 規格例：運転免許証 NFC-B（ISO 14443-3B）
                arrayOf(android.nfc.tech.NfcF::class.java.name),            // 規格例：Suica,ICOCA,楽天Edy,入館証 NFC-F（JIS 6319-4）
                arrayOf(android.nfc.tech.NfcV::class.java.name),            // NFC-V（ISO 15693）
                arrayOf(android.nfc.tech.Ndef::class.java.name),            // NDEF としてフォーマットされた NFC タグの NDEF データとオペレーションへのアクセスを提供
                arrayOf(android.nfc.tech.NdefFormatable::class.java.name))  // NDEF にフォーマット可能なタグに対するフォーマット オペレーション

        mNfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)

        // ボタン、テキスト変数作成
        textMessage_read = findViewById<TextView>(R.id.textView_ReadMode)
        textMessage_write = findViewById<TextView>(R.id.textView_WriteMode)
        togglebutton_read = findViewById<CompoundButton>(R.id.toggleButton_Read)
        togglebutton_write = findViewById<CompoundButton>(R.id.toggleButton_Write)

    }

    override fun onResume() {
        super.onResume()

        // 変数作成
        var mode_RW: Int = 0;

        // NFCタグの検出を有効化
        mNfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)


        /* togglebutton処理*/
        togglebutton_read?.setOnCheckedChangeListener { _, isCheckked ->
            if(isCheckked){
                Toast.makeText(applicationContext, "Read_ONになりました。メッセージの書き込み.Mode = 0", Toast.LENGTH_LONG).show()
                NFCmodeflag = 0;
                textMessage_write?.text = text
            }
            else {
                Toast.makeText(applicationContext, "Read_OFFになりました。メッセージの読み込みモードです。Mode = 1", Toast.LENGTH_LONG).show()
                NFCmodeflag = 1;
                textMessage_write?.text = ""
            }
        }
        togglebutton_write?.setOnCheckedChangeListener { _, isCheckked ->
            if(isCheckked){
                Toast.makeText(applicationContext, "Write_ONになりました。タグIDを取得。Mode = 2", Toast.LENGTH_LONG).show()
                NFCmodeflag = 2;
                textMessage_read?.text = ""
            }
            else {
                Toast.makeText(applicationContext, "OFFになりました。Mode = 3", Toast.LENGTH_LONG).show()
                NFCmodeflag = 3;
            }
        }
    }

    /**
     * NFCタグの検出時に呼ばれる
     */
    override fun onNewIntent(intent: Intent) {
        if(NFCmodeflag == 0) {      /* NDEFメッセージの書き込み*/
            this.WriteNFCNDEF(intent)
        }
        else if (NFCmodeflag == 1){      /* NDEFメッセージの読込*/
            this.ReadNFCNDEF(intent)
        }
        else if (NFCmodeflag == 2) {    /*tagIDの読み込み*/
            this.ReadNFCTagID(intent)
        }
        else {
            /* 特に何もしない */
        }
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter?.disableForegroundDispatch(this)
    }

    private fun ReadNFCNDEF(intent: Intent){
        if(NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
            || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
            val ndef = Ndef.get(tag) ?: return
            val raws = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return
            var msgs = arrayOfNulls<NdefMessage>(raws.size)

            for (i in raws.indices) {
                msgs[i] = raws[i] as NdefMessage?
                if (msgs[i] != null) {
                    for (record in msgs[i]?.records!!) {

                        Log.d("TAG", "TNF：" + record.tnf)
                        Log.d("TAG", "Type：" + String(record.type))

                        // payload（データ本体）
                        Log.d("TAG", "payload：" + String(record.payload))
                        textMessage_read?.text = String(record.payload)
                        // payloadからメッセージ部分を抽出
                        Log.d(
                            "TAG",
                            "payload-message：" + String(
                                record.payload,
                                3,
                                record.payload.size - 3
                            )
                        )

                        // payloadの中身を1byteずつ表示
                        for (i in record.payload.indices) {
                            Log.d(
                                "TAG", String.format(
                                    "payload[%d] : 0x%02x / %c",
                                    i,
                                    record.payload[i].toInt() and 0xFF,
                                    record.payload[i].toInt() and 0xFF
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun WriteNFCNDEF(intent: Intent){
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
            || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
            val ndef = Ndef.get(tag) ?: return

            if (ndef.isWritable) {
                val record = NdefRecord.createTextRecord("en", text)
                val msg = NdefMessage(record);

                ndef.connect()
                ndef.writeNdefMessage(msg)
                ndef.close()
            }
        }
    }


    private fun ReadNFCTagID(intent: Intent){
        // タグのIDを取得
        val tagId: ByteArray = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return

        var list = ArrayList<String>()
        for (byte in tagId) {
            list.add(String.format("%02X", byte.toInt() and 0xFF))
        }

        //val Byte = NfcAdapter.EXTRA_NDEF_MESSAGES.toByteArray();

        // 画面に表示
        textMessage_read?.text = list.joinToString(":")
    }

}
