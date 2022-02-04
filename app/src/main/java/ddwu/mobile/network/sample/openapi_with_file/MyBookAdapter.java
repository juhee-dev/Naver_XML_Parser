package ddwu.mobile.network.sample.openapi_with_file;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ddwu.mobile.network.sample.openapi_with_file.R;

public class MyBookAdapter extends BaseAdapter {

    public static final String TAG = "MyBookAdapter";

    private LayoutInflater inflater;
    private Context context;
    private int layout;
    private ArrayList<NaverBookDto> list;
    private NaverNetworkManager networkManager = null;
    private ImageFileManager imageFileManager = null;


    public MyBookAdapter(Context context, int resource, ArrayList<NaverBookDto> list) {
        this.context = context;
        this.layout = resource;
        this.list = list;
        imageFileManager = new ImageFileManager(context);
        networkManager = new NaverNetworkManager(context);
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return list.size();
    }


    @Override
    public NaverBookDto getItem(int position) {
        return list.get(position);
    }


    @Override
    public long getItemId(int position) {
        return list.get(position).get_id();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Log.d(TAG, "getView with position : " + position);
        View view = convertView;
        ViewHolder viewHolder = null;

        if (view == null) {
            view = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = view.findViewById(R.id.tvTitle);
            viewHolder.tvAuthor = view.findViewById(R.id.tvAuthor);
            viewHolder.ivImage = view.findViewById(R.id.ivImage);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        NaverBookDto dto = list.get(position);

        viewHolder.tvTitle.setText(dto.getTitle());
        viewHolder.tvAuthor.setText(dto.getAuthor());

//         dto의 이미지 주소에 해당하는 이미지 파일이 내부저장소에 있는지 확인
//         ImageFileManager 의 내부저장소에서 이미지 파일 읽어오기 사용
//         이미지 파일이 있을 경우 bitmap, 없을 경우 null 을 반환하므로 bitmap 이 있으면 이미지뷰에 지정
//         없을 경우 GetImageAsyncTask 를 사용하여 이미지 파일 다운로드 수행

        if (dto.getImageLink() == null) { // 원래 데이터가 이미지를 갖고있지 않을 수도 있으므로
            viewHolder.ivImage.setImageResource(R.mipmap.ic_launcher); // 기본 이미지 출력
            return view;
        }

        // 파일에 있는지 확인하여 있으면 가져오고 없으면 네트워크에서 가져오기
        // dto 의 이미지 주소 정보로 이미지 파일 읽기
        Bitmap savedBitmap = imageFileManager.getBitmapFromTemporary(dto.getImageLink()); // 이미지 주소에서 이미지 파일 이름을 꺼내와 캐시에 저장되어 있는지 검사

        if (savedBitmap != null) { // 이미지 파일이 이미 저장되어 있으면 보여준다
            viewHolder.ivImage.setImageBitmap(savedBitmap);
            Log.d(TAG, "Image loading from file");
        } else { // 이미지 파일이 없으면 네트워크에서 가져와 보여준다
            viewHolder.ivImage.setImageResource(R.mipmap.ic_launcher);
            new GetImageAsyncTask(viewHolder).execute(dto.getImageLink());
            Log.d(TAG, "Imgae loading from network");
        }

        return view;
    }


    public void setList(ArrayList<NaverBookDto> list) {
        this.list = list;
        notifyDataSetChanged();
    }

//    ※ findViewById() 호출 감소를 위해 필수로 사용할 것
    static class ViewHolder {
        public TextView tvTitle = null;
        public TextView tvAuthor = null;
        public ImageView ivImage = null;
    }


   /* 책 이미지를 다운로드 후 내부저장소에 파일로 저장하고 이미지뷰에 표시하는 AsyncTask */
    // 1. 네트워크에서 이미지 다운
    // 2. 뷰홀더에 이미지 설정
    // 3. 이미지 파일 저장
    class GetImageAsyncTask extends AsyncTask<String, Void, Bitmap> {

        ViewHolder viewHolder;
        String imageAddress;

//        현재 사용하고 있는 뷰홀더를 저장해두어야 뒤죽박죽되지 않는다
        public GetImageAsyncTask(ViewHolder holder) {
            viewHolder = holder;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            imageAddress = params[0];
            Bitmap result = null;
            result = networkManager.downloadImage(imageAddress);
            return result;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            /*네트워크에서 다운 받은 이미지 파일을 ImageFileManager 를 사용하여 내부저장소에 저장
            * 다운받은 bitmap 을 이미지뷰에 지정*/
            if (bitmap != null) {
                viewHolder.ivImage.setImageBitmap(bitmap); // 뷰홀더에 이미지 저장
                imageFileManager.saveBitmapToTemporary(bitmap, imageAddress); // 내부 저장소에 임시로 이미지 저장
            }
        }

    }

}
