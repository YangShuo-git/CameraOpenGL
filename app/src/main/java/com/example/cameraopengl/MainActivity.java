package com.example.cameraopengl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cameraopengl.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glSurfaceView;

    // 1. 加载so库.
    static {
        System.loadLibrary("cameraopengl");
    }

    private ActivityMainBinding binding;
    private String name = "baiyang";  // 被C++调用的参数：名字和年龄（需要签名）
    public static int age = 28;

    // 2. java层定义接口  native关键字，说明是要调用jni的接口
    public native String stringFromJNI();
    public native String stringGetJNI();
    public native String stringSetJNI(String name);  // 自动生成对应的Native代码函数
    static public native String staticStringFromJNI();
    public native void changeName();
    public static native void changeAge();
    public native void callAddMethod();
    public native void callShowStringMethod();

    // 被C++调用的函数（需要签名，解决函数重载的问题） (II)I
    public int add(int num1, int num2){
        return num1 + num2;
    }

    // (Ljava/lang/String)Ljava/lang/String
    public String showString(String str){
        return "【" + str + "】";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        //setContentView(binding.getRoot());
        setContentView(R.layout.activity_main);
        checkPermission();

        //initView();
    }

    public void jniFunction() {
        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        TextView tv2 = binding.sampleText2;
        tv2.setText(stringGetJNI());

        TextView tv3 = binding.sampleText3;
        tv3.setText(stringSetJNI("hello native"));

        TextView tv4 = binding.sampleText4;
        tv3.setText(staticStringFromJNI());

        System.out.println("修改之前：name: " + name + ", age: " + age);
        changeName();
        changeAge();
        System.out.println("修改之后：name: " + name + ", age: " + age);

        callAddMethod();
        callShowStringMethod();
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
        return false;
    }

    private void initView() {
        // 将glSurfaceView与ui的绑定
        glSurfaceView = (GLSurfaceView)findViewById(R.id.glsurfaceview);
        // 设置GLContext的版本
        glSurfaceView.setEGLContextClientVersion(2);
        // 设置Render
        glSurfaceView.setRenderer(new TriangleRender());
        //glSurfaceView.setRenderer(new CameraRender());
        // 设置渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染;RENDERMODE_CONTINUOUSLY表示持续渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        glSurfaceView.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        glSurfaceView.onPause();
//    }
}