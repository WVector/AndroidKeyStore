package com.vector.androidkeystore;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.security.KeyPairGeneratorSpec;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

import static com.vector.androidkeystore.R.id.aliasText;
import static com.vector.androidkeystore.R.id.decryptedText;
import static com.vector.androidkeystore.R.id.encryptedText;
import static com.vector.androidkeystore.R.id.startText;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private KeyStore mKeyStore;
    private ArrayList<String> keyAliases;
    private ListAdapter listAdapter;
    private TextView mAliasText;
    private EditText mStartText;
    private EditText mEncryptedText;
    private EditText mDecryptedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.listView);
        View header = getLayoutInflater().inflate(R.layout.listview_header, null);

        mAliasText = (TextView) header.findViewById(aliasText);
        Button generateKeyPair = (Button) header.findViewById(R.id.generateKeyPair);
        generateKeyPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewKeys();
            }
        });
        mStartText = (EditText) header.findViewById(startText);
        mEncryptedText = (EditText) header.findViewById(encryptedText);
        mDecryptedText = (EditText) header.findViewById(decryptedText);

        listView.addHeaderView(header);


        initKeyStore();
        refreshKeys();

        listAdapter = new ListAdapter();
        listView.setAdapter(listAdapter);

    }

    private void initKeyStore() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createNewKeys() {
        String alias = mAliasText.getText().toString();
        try {
            // Create new key if needed
            if (!mKeyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(this)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                generator.initialize(spec);

                KeyPair keyPair = generator.generateKeyPair();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
        refreshKeys();
    }

    private void refreshKeys() {
        keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = mKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        } catch (Exception e) {
        }

        if (listAdapter != null)
            listAdapter.notifyDataSetChanged();
    }

    class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return keyAliases.size();
        }

        @Override
        public String getItem(int position) {
            return keyAliases.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.listview_item, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.init(getItem(position));
            return convertView;
        }


    }

    class ViewHolder {

        private final Button mDecryptButton;
        private final Button mEncryptButton;
        private final Button mDeleteButton;
        private final TextView mKeyAlias;

        public ViewHolder(View view) {
            mKeyAlias = (TextView) view.findViewById(R.id.keyAlias);
            mDeleteButton = (Button) view.findViewById(R.id.deleteButton);
            mEncryptButton = (Button) view.findViewById(R.id.encryptButton);
            mDecryptButton = (Button) view.findViewById(R.id.decryptButton);
        }

        public void init(final String alias) {
            if (!TextUtils.isEmpty(alias)) {
                mKeyAlias.setText(alias);
            }

            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteKey(alias);
                }
            });

            mEncryptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    encryptString(alias);

                }
            });

            mDecryptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    decryptString(alias);
                }
            });

        }
    }

    public void deleteKey(final String alias) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Key")
                .setMessage("Do you want to delete the key \"" + alias + "\" from the keystore?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mKeyStore.deleteEntry(alias);
                            refreshKeys();
                        } catch (KeyStoreException e) {
                            Toast.makeText(MainActivity.this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    public void encryptString(String alias) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) mKeyStore.getEntry(alias, null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            String initialText = mStartText.getText().toString();
            if (initialText.isEmpty()) {
                Toast.makeText(this, "Enter text in the 'Initial Text' widget", Toast.LENGTH_LONG).show();
                return;
            }

            Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, input);
            cipherOutputStream.write(initialText.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte[] vals = outputStream.toByteArray();
            String text = Base64.encodeToString(vals, Base64.DEFAULT);
            Log.d(TAG, text);
            mEncryptedText.setText(text);
        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void decryptString(String alias) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) mKeyStore.getEntry(alias, null);
//            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            String cipherText = mEncryptedText.getText().toString();
            CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            String finalText = new String(bytes, 0, bytes.length, "UTF-8");
            mDecryptedText.setText(finalText);

        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
