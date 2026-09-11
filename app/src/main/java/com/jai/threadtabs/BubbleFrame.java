package com.jai.threadtabs;

import android.content.Context;
import android.graphics.*;
import android.media.MediaPlayer;
import android.view.*;
import android.widget.FrameLayout;
import org.json.JSONObject;

/** Media remains inside this message; one explicitly selected background video plays at a time. */
final class BubbleFrame extends FrameLayout implements TextureView.SurfaceTextureListener {
    private final String media; private MediaPlayer player; private Surface surface;
    private final Paint paint=new Paint(3); private final Path clip=new Path();
    private Bitmap image; private final int tint,dim; private final float radius;
    private static final android.util.LruCache<String,Bitmap> CACHE=new android.util.LruCache<String,Bitmap>(12*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getByteCount();}};
    BubbleFrame(Context c,JSONObject s,int fallback,boolean play){
        super(c);setWillNotDraw(false);media=s.optString("media","");tint=Appearance.color(s,"bubble",fallback);dim=s.optInt("dim",50);radius=s.optInt("radius",18)*getResources().getDisplayMetrics().density;
        String thumb=s.optString("kind","").equals("video")?media+".jpg":media;
        if(!thumb.isEmpty()){
            image=CACHE.get(thumb);
            if(image==null){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=2;image=BitmapFactory.decodeFile(thumb,opts);if(image!=null)CACHE.put(thumb,image);}
        }
        if(play&&s.optString("kind").equals("video")&&!media.isEmpty()){
            TextureView video=new TextureView(c);video.setOpaque(false);video.setSurfaceTextureListener(this);addView(video,new LayoutParams(-1,-1));
        }
    }
    @Override public void draw(Canvas c){int save=c.save();Path outline=new Path();outline.addRoundRect(0,0,getWidth(),getHeight(),radius,radius,Path.Direction.CW);c.clipPath(outline);super.draw(c);c.restoreToCount(save);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(tint);if(image!=null)c.drawBitmap(image,null,new Rect(0,0,getWidth(),getHeight()),paint);}
    @Override protected void dispatchDraw(Canvas c){
        int save=c.save();clip.reset();clip.addRoundRect(0,0,getWidth(),getHeight(),radius,radius,Path.Direction.CW);c.clipPath(clip);
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            if(!(child instanceof TextureView)&&!media.isEmpty())c.drawColor(Color.argb(Math.round(dim*2.55f),0,0,0));
            drawChild(c,child,getDrawingTime());
        }c.restoreToCount(save);
    }
    public void onSurfaceTextureAvailable(SurfaceTexture texture,int w,int h){
        try{surface=new Surface(texture);player=new MediaPlayer();player.setDataSource(media);player.setSurface(surface);player.setLooping(true);player.setVolume(0,0);player.setOnPreparedListener(p->p.start());player.setOnErrorListener((p,a,b)->{release();return true;});player.prepareAsync();}catch(Exception e){release();}
    }
    private void release(){if(player!=null){player.release();player=null;}if(surface!=null){surface.release();surface=null;}}
    public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){}public void onSurfaceTextureUpdated(SurfaceTexture t){}
    public boolean onSurfaceTextureDestroyed(SurfaceTexture t){release();return true;}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
