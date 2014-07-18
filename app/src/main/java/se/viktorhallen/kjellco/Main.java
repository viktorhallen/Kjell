package se.viktorhallen.kjellco;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends Activity {
    private static final String DEBUG_TAG = "HttpExample";
    private EditText urlText;
    private TextView textView;
    private CookieManager cookieManager;
    //todo
    private boolean debug = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlText = (EditText) findViewById(R.id.artnr_editText);
        textView = (TextView) findViewById(R.id.textView);
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);


        String[] cookie_key = {"SelectedStore","FilterInStockSelectedStore","IsLoggedIn", "UserId", "LastUserId"};
        String[] cookie_value = {"24","24","1","{ea48fc81-74c3-4a7d-80f4-433ff1168004}","{ea48fc81-74c3-4a7d-80f4-433ff1168004}"};
        for (int i = 0; i<cookie_key.length; i++ ) {
            if (debug) Log.d(DEBUG_TAG, "Added cookie[" + i + "]: {"+cookie_key[i]+"="+cookie_value[i]+"}");
            HttpCookie cookie = new HttpCookie(cookie_key[i], cookie_value[i]);
            cookie.setDomain("kjell.com");
            cookie.setPath("/");
            cookie.setVersion(0);
            try {
                cookieManager.getCookieStore().add(new URI("http://kjell.com/"), cookie);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            myClickHandler(textView);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // When user clicks button, calls AsyncTask.
    // Before attempting to fetch the URL, makes sure that there is a network connection.
    public void myClickHandler(View view) {
        textView.setText("Wait...");
        // Gets the URL from the UI's text field.
        String stringUrl = "http://kjell.com/Inkopslistor/Shoppinglists?productId=130399711315545015";//http://kjell.com/"+urlText.getText().toString(); //"http://kjell.com/Inkopslistor/Shoppinglists?productId=130399711315545015";//urlText.getText().toString();
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask().execute(stringUrl);
        } else {
            textView.setText("No network connection available.");
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class DownloadWebpageTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                String five = "5";
                publishProgress(five);
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }

        }
        /** This method runs on the UI thread */
        @Override
        protected void onProgressUpdate(String... progressValue) {
            textView.setText("No match found.");
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Pattern pattern =
                    Pattern.compile("(?<=<title>)([\\w &;]+)(?= \\| Kjell.com<\\/title>)");//(urlText.getText().toString());//("/<p class=\"artNbr\">Art nr: (\\d{5})</p>/");

            Matcher matcher =
                    pattern.matcher(result);

            boolean found = false;
            while (matcher.find()) {
                textView.setText(matcher.group() + "\n");
                found = true;
            }
            if(!found){
                textView.setText("No match found.");
            }
            textView.setText(textView.getText() + "\n" + result);
            if (debug) Log.d(DEBUG_TAG, result);
        }
        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as
        // a string.
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 5000;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                if (debug) Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                return contentAsString;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        // Reads an InputStream and converts it to a String.
        public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }
    }

}
