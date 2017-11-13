package com.hcoderteam.hcoder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.hcoderteam.hcoder.Huffman.BitInputStream;
import com.hcoderteam.hcoder.Huffman.BitOutputStream;
import com.hcoderteam.hcoder.Huffman.FrequencyTable;
import com.hcoderteam.hcoder.Huffman.Huffman;
import com.hcoderteam.hcoder.Huffman.HuffmanTree;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppCompatButton send_button;
    private ListView send_listview;
    private AppCompatButton get_button;
    private ListView get_listview;

    private static final int CHOOSE_FILE_REQUEST = 0;

    private static final int CHOOSE_FILE = 10;
    private static final int SEND_FILE = 11;
    private int send_status = CHOOSE_FILE;

    private static final int REFRESH = 20;
    private static final int GET_FILE = 21;
    private int get_status = REFRESH;

    private AlertDialog alertDialog;
    private RequestQueue requestQueue;
    private String username;

    private ArrayList<String> active_users = new ArrayList<>();
    private ArrayList<InboxFile> inboxFilesList = new ArrayList<>();
    private ArrayList<String> inbox_files = new ArrayList<>();

    private Uri chosenFileUri = null;

    private HuffmanTree ht;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        username = getIntent().getStringExtra("USERNAME");

        send_button = (AppCompatButton) findViewById(R.id.send_button);
        send_listview = (ListView) findViewById(R.id.send_listview);

        get_button = (AppCompatButton) findViewById(R.id.get_button);
        get_listview = (ListView) findViewById(R.id.get_listview);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.DialogTheme));
        builder.setTitle("Loading Active Users")
                .setMessage("")
                .setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (send_status == CHOOSE_FILE) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/plain");
                    startActivityForResult(intent, CHOOSE_FILE_REQUEST);
                } else {

                    compressAndSendChosenFile();
                }
            }
        });

        get_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (get_status == REFRESH) {
                    get_button.setEnabled(false);
                    get_button.setText("Refreshing");

                    try {
                        String url = getString(R.string.server_url) + "/checkinbox";
                        String json_string = "{\"username\":\"" + username + "\"}";
                        JSONObject json = new JSONObject(json_string);

                        JsonObjectRequest check_inbox_request = new JsonObjectRequest(Request.Method.POST, url,
                                json, check_inbox_listener, check_inbox_error_listener);

                        Volley.newRequestQueue(getApplicationContext()).add(check_inbox_request);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                    getChosenFileAndSave();
                }
            }
        });

        try {
            String url = getString(R.string.server_url) + "/getactiveusers";
            String json_string = "{\"username\":\"" + username + "\"}";
            JSONObject json = new JSONObject(json_string);

            JsonObjectRequest active_users_request = new JsonObjectRequest(Request.Method.POST, url,
                    json, active_users_listener, active_users_error_listener);

            Volley.newRequestQueue(getApplicationContext()).add(active_users_request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_single_choice, android.R.id.text1, active_users);
        send_listview.setAdapter(adapter);
        send_listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        send_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("LOGG", ""+send_listview.getCheckedItemPosition());
            }
        });

        final ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_single_choice, android.R.id.text1, inbox_files);
        get_listview.setAdapter(adapter2);
        get_listview.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        get_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("LOGG", ""+get_listview.getCheckedItemPosition());
            }
        });
    }

    class InboxFile {
        int id;
        String sender;
        String receiver;
        String filename;

        public InboxFile(int id, String sender, String receiver, String filename) {
            this.id = id;
            this.sender = sender;
            this.receiver = receiver;
            this.filename = filename;
        }
    }

    private Response.Listener<JSONObject> active_users_listener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            alertDialog.dismiss();

            try {
                JSONArray active_users_array = response.getJSONArray("active_users");
                for (int i=0; i<active_users_array.length(); i++)
                    active_users.add(active_users_array.getString(i));

                if (active_users.size() > 0)
                    send_listview.setItemChecked(0,true);
                else
                    Toast.makeText(getApplicationContext(), "There is No Active User", Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Response.ErrorListener active_users_error_listener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {

            alertDialog.dismiss();

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getApplicationContext(), R.style.DialogTheme));
            builder.setTitle("Failed")
                    .setMessage("")
                    .setCancelable(false)
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                            finish();
                        }
                    });
            alertDialog = builder.create();
            alertDialog.show();
        }
    };

    private Response.Listener<JSONObject> check_inbox_listener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            get_button.setEnabled(true);

            try {
                JSONArray inbox_files_array = response.getJSONArray("files");
                for (int i=0; i<inbox_files_array.length(); i++) {
                    JSONObject file_json = inbox_files_array.getJSONObject(i);
                    inboxFilesList.add(new InboxFile(
                            file_json.getInt("id"),
                            file_json.getString("sender"),
                            file_json.getString("receiver"),
                            file_json.getString("file_name")));
                    inbox_files.add(file_json.getString("sender"));
                }
                if (inboxFilesList.size() > 0) {
                    get_listview.setItemChecked(0, true);

                    get_button.setText("Get File");
                    get_status = GET_FILE;
                } else {
                    get_button.setText("Refresh");
                    Toast.makeText(getApplicationContext(), "There is No New File", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Response.ErrorListener check_inbox_error_listener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            get_button.setEnabled(true);
            get_button.setText("Refresh");

            Toast.makeText(getApplicationContext(), "Error",Toast.LENGTH_LONG).show();
        }
    };

    private void compressAndSendChosenFile() {

        final String receiver = active_users.get(send_listview.getCheckedItemPosition());

        try {
            InputStream chosenFileInStream = getApplicationContext().getContentResolver().openInputStream(chosenFileUri);
            InputStreamReader chosenFileInReader = new InputStreamReader(chosenFileInStream);

            StringBuilder sb = new StringBuilder();

            int cr;
            while ((cr = chosenFileInReader.read()) != -1)
                sb.append((char)cr);

            String chosenFileInputString = sb.toString();

            FrequencyTable frequencyTable = Huffman.getFrequencyTable(chosenFileInputString);
            HuffmanTree huffmanTree = Huffman.buildTree(frequencyTable);
            ht = huffmanTree;
            //Huffman.printCodes(huffmanTree, new StringBuffer());
            String encoded = Huffman.encode(huffmanTree, chosenFileInputString);
            String padded_encoded = Huffman.add_padding(encoded);

            FileOutputStream encodedOutputStream = openFileOutput("encoded.bin", Context.MODE_PRIVATE);
            BitOutputStream encodedBitOutputStream = new BitOutputStream(encodedOutputStream);

            int length = padded_encoded.length();
            for (int i=0; i<length; i++) {
                encodedBitOutputStream.write((int)padded_encoded.charAt(i) - 48);
            }
            encodedBitOutputStream.close();

            int bytecount = padded_encoded.length()/8;
            final byte[] bytes = new byte[bytecount];
            BufferedInputStream bis = new BufferedInputStream(openFileInput("encoded.bin"));
            bis.read(bytes, 0, bytes.length);
            bis.close();

            Log.d("LOGG", "ENCODED\n"+encoded);
            Log.d("LOGG", "ENCODED_LENGTH: "+encoded.length());
            Log.d("LOGG", "ENCODED_PADDED\n"+padded_encoded);
            Log.d("LOGG", "PADDED_ENCODED_LENGTH: "+padded_encoded.length());

            HashMap<Character, String> codes = Huffman.getCodes(huffmanTree);
            StringBuilder stringBuilder = new StringBuilder();
            for (char c : codes.keySet()) {
                String code = c + "-" + codes.get(c) + "\n";
                stringBuilder.append(code);
            }
            final String codesString = stringBuilder.toString();

            String url = getString(R.string.server_url) + "/sendfile";
            VolleyMultipartRequest send_file_request = new VolleyMultipartRequest(Request.Method.POST, url,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            Toast.makeText(getApplicationContext(), "File Has Been Sended", Toast.LENGTH_LONG).show();

                            send_button.setText("Choose File");
                            send_status = CHOOSE_FILE;
                            send_button.setEnabled(true);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();

                            send_button.setText("Send File");
                            send_button.setEnabled(true);
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Content-Type", "multipart/form-data");
                    params.put("Connection", "Keep-Alive");
                    params.put("Sender", username);
                    params.put("Receiver", receiver);
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() throws AuthFailureError {
                    Map<String, DataPart> params = new HashMap<>();

                    params.put("huffman", new DataPart("encoded.bin", bytes));
                    return params;
                }
            };

            send_file_request.setRetryPolicy(new DefaultRetryPolicy(10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(send_file_request);
            send_button.setEnabled(false);
            send_button.setText("Sending");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getChosenFileAndSave() {
        final InboxFile inboxFile = inboxFilesList.get(get_listview.getCheckedItemPosition());

        String url = getString(R.string.server_url) + "/getfile";
        VolleyInputStreamRequest get_file_request = new VolleyInputStreamRequest(
                Request.Method.POST, url,
                new Response.Listener<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {

                        try {
                            BitInputStream bitInputStream = new BitInputStream(new BufferedInputStream(new ByteArrayInputStream(response)));

                            StringBuilder stringBuilder = new StringBuilder();

                            int r;
                            while ((r = bitInputStream.read()) != -1)
                                stringBuilder.append(Integer.toString(r));

                            String padded_undecoded = stringBuilder.toString();
                            String undecoded = Huffman.remove_padding(padded_undecoded);
                            String decoded = Huffman.decode(ht, undecoded);

                            Log.d("LOGG", "PADDED_UNDECODED\n"+padded_undecoded);
                            Log.d("LOGG", "PADDED_UNDECODED_LENGTH: "+padded_undecoded.length());
                            Log.d("LOGG", "UNDECODED\n"+undecoded);
                            Log.d("LOGG", "UNDECODED_LENGTH: "+undecoded.length());
                            Log.d("LOGG", "DECODED\n"+decoded);
                            Log.d("LOGG", "DECODED_LENGTH: "+decoded.length());


                            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File directory = new File(downloads.getAbsolutePath()+"/HCoder");
                            boolean success = true;
                            if (!directory.exists())
                                success = directory.mkdirs();
                            if (success) {
                                File file = new File(directory, "text.txt");
                                FileOutputStream fos = new FileOutputStream(file);

                                byte[] contentInBytes = decoded.getBytes();

                                fos.write(contentInBytes);
                                fos.flush();
                                fos.close();

                            } else {
                                Toast.makeText(getApplicationContext(), "DOSYA KAYDEDILEMEDI", Toast.LENGTH_LONG).show();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();

                    }
                }, null) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "multipart/form-data");
                params.put("Connection", "Keep-Alive");
                params.put("Id", Integer.toString(inboxFile.id));
                params.put("Sender", inboxFile.sender);
                params.put("Receiver", inboxFile.receiver);
                params.put("FileName", inboxFile.filename);
                return params;
            }
        };

        requestQueue.add(get_file_request);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.DialogTheme));
        builder.setTitle("Do you want logout?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("LOGG", "rq= "+resultCode+" can_code= "+RESULT_CANCELED);

        if( requestCode == CHOOSE_FILE_REQUEST) {
            if (resultCode == RESULT_OK) {

                chosenFileUri = data.getData();
                send_button.setText("Send File");
                send_status = SEND_FILE;

                Uri fileURI = data.getData();
/*
                try {


                    BitInputStream bitInputStream = new BitInputStream(new BufferedInputStream(openFileInput("encoded.bin")));
                    StringBuilder stringBuilder = new StringBuilder();

                    int r;
                    while ((r = bitInputStream.read()) != -1)
                        stringBuilder.append(Integer.toString(r));

                    String padded_undecoded = stringBuilder.toString();

                    Log.d("LOGG", "PADDED_UNDECODED\n"+padded_undecoded);

                    Log.d("LOGG", "PADDED_UNDECODED_LENGTH: "+padded_undecoded.length());

                    String undecoded = Huffman.remove_padding(padded_undecoded);

                    Log.d("LOGG", "UNDECODED\n"+undecoded);

                    Log.d("LOGG", "UNDECODED_LENGTH: "+undecoded.length());

                    String decoded = Huffman.decode(huffmanTree, undecoded);

                    Log.d("LOGG", "DECODED\n"+decoded);

                    Log.d("LOGG", "DECODED_LENGTH: "+decoded.length());

                    File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File directory = new File(downloads.getAbsolutePath()+"/HCoder");
                    boolean success = true;
                    if (!directory.exists())
                        success = directory.mkdirs();
                    if (success) {
                        File file = new File(directory, "text.txt");
                        FileOutputStream fos = new FileOutputStream(file);

                        byte[] contentInBytes = decoded.getBytes();

                        fos.write(contentInBytes);
                        fos.flush();
                        fos.close();

                    } else {
                        Toast.makeText(getApplicationContext(), "DOSYA KAYDEDILEMEDI", Toast.LENGTH_LONG).show();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
*/
            }
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
