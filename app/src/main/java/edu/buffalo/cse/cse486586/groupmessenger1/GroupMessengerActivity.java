package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentValues;
import android.os.Bundle;
import android.app.Activity;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.content.Context;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.widget.EditText;
import android.view.View.OnKeyListener;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String remote_ports[] = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    static final int SERVER_PORT = 10000;
    static String ATTR_key;
    static String ATTR_value;
    static int MSG_SEQNO = 0;
    static String myPort;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
    /*
        Determine the port no on which AVD listens
     */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ATTR_key = new String("key");
        ATTR_value = new String("value");
         /*
        Server socket to listen to for incoming connections
         */
        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (Exception ex) {
            Log.e("Server_Socket", "Cannot create server socket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket sock = null;
            String message;
            while (true) {
                try {
                    sock = serverSocket.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));   //read message from input stream.
                    message = br.readLine();
                    ContentValues cv = new ContentValues(); //create a ContentValues object to insert values into the ContentResolver for use in insert
                    cv.put(ATTR_key, MSG_SEQNO);
                    cv.put(ATTR_value, message);
                    MSG_SEQNO++;    //increment sequence number by 1
                    getContentResolver().insert(GroupMessengerProvider.URI_obj, cv);
                    publishProgress(message);
                    sock.close();   //close the socket.
                } catch (IOException err) {
                    Log.e("ServerTask", "Server failed");
                }
            }
        }
        protected void onProgressUpdate(String...strings) {
             /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... msgs) {
            try {
                int i=0;
                String msgToSend = msgs[0];
                for(i=0;i<remote_ports.length;i++) {    //send message to all the remote ports, including itself
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_ports[i]));
                    BufferedWriter send_msg = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); //write the message received to the output stream
                    send_msg.write(msgToSend);
                    send_msg.flush();   //force invoke flush to send message and clear buffer.
                    socket.close(); //close the socket.
                }
            } catch (UnknownHostException e) {
                Log.e("ClientTask", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("ClientTask", "ClientTask socket IOException");
            }
            return null;
        }
    }

}
