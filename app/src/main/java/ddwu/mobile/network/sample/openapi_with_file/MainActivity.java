package ddwu.mobile.network.sample.openapi_with_file;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import ddwu.mobile.network.sample.openapi_with_file.R;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    EditText etTarget;
    ListView lvList;
    String apiAddress;

    String query;

    MyBookAdapter adapter;
    ArrayList<NaverBookDto> resultList;
    NaverBookXmlParser parser;
    NaverNetworkManager networkManager;
    ImageFileManager imgFileManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTarget = findViewById(R.id.etTarget);
        lvList = findViewById(R.id.lvList);

        resultList = new ArrayList();
        adapter = new MyBookAdapter(this, R.layout.listview_book, resultList);
        lvList.setAdapter(adapter);

        apiAddress = getResources().getString(R.string.api_url);
        parser = new NaverBookXmlParser();
        networkManager = new NaverNetworkManager(this);
        networkManager.setClientId(getResources().getString(R.string.client_id));
        networkManager.setClientSecret(getResources().getString(R.string.client_secret)); // 네이버 api 이용할 때에만 필요한 정보
        imgFileManager = new ImageFileManager(this);

        lvList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /* 작성할 부분 */
                /*롱클릭한 항목의 이미지 주소를 가져와 내부 메모리에 지정한 이미지 파일을 외부저장소로 이동
                * ImageFileManager 의 이동 기능 사용
                * 이동을 성공할 경우 파일 명, 실패했을 경우 null 을 반환하므로 해당 값에 따라 Toast 출력*/
                String url = resultList.get(position).getImageLink();
                String rslt = imgFileManager.moveFileToExt(url);
                Toast.makeText(MainActivity.this, rslt, Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }


    @Override
    protected void onDestroy() { // 앱이 닫힐 때
        super.onDestroy();
        // 임시 파일 삭제
        imgFileManager.clearTemporaryFiles();
    }


    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnSearch:
                query = etTarget.getText().toString();  // UTF-8 인코딩 필요
                // OpenAPI 주소와 query 조합 후 서버에서 데이터를 가져옴
                // 가져온 데이터는 파싱 수행 후 어댑터에 설정
                try { // 인코딩 오류가 날 수 있으므로 try-catch 잡아줘야
                    new NetworkAsyncTask().execute(apiAddress +
                            URLEncoder.encode(query, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    class NetworkAsyncTask extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDlg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDlg = ProgressDialog.show(MainActivity.this, "Wait", "Downloading...");
        }

        @Override
        protected String doInBackground(String... strings) {
            String address = strings[0];
            String result = null;
            // networking
            result = networkManager.downloadContents(address);
            if (result == null) return "Error!";

            Log.d(TAG, result);

            // parsing - 수행시간이 많이 걸릴 경우 이곳(스레드 내부)에서 수행하는 것을 고려
            resultList = parser.parse(result);
            // parsing 을 이곳에서 수행할 경우 AsyncTask의 반환타입을 적절히 변경
            // 이미지 표현 방법2. 처음 실행할 때 모든 이미지를 파일에 저장하는 방법
//            for (NaverBookDto dto : resultList) {
//                Bitmap bitmap = networkManager.downloadImage(dto.getImageLink());
//                // 이미지 있는 경우에만 이미지를 파일에 저장
//                if (bitmap != null) imgFileManager.saveBitmapToTemporary(bitmap, dto.getImageLink());
//            }

            return result;
        }


        @Override
        protected void onPostExecute(String result) {
            // parsing - 수행시간이 짧을 경우 이 부분에서 수행하는 것을 고려
//            resultList = parser.parse(result);
            adapter.setList(resultList);    // Adapter 에 결과 List 를 설정 후 notify
            progressDlg.dismiss();
        }

    }

}
