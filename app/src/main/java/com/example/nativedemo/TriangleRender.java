package com.example.nativedemo;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TriangleRender implements GLSurfaceView.Renderer {
    private int mProgram;
    private FloatBuffer vertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;

    // CPU的数据往vPosition里塞，vPosition再传给GPU的内置变量
    private final String vertexShaderCode =
            "attribute vec4 vPosition; " +
                    "void main() {" +
                    "   gl_Position = vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // 设置顶点形状
    static float triangleCoords[] = {
            0.5f,  0.5f, 0.0f,  // top
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f   // bottom right
    };
    //设置颜色，依次为红绿蓝和透明通道
    float color[] = { 0.5f, 1.0f, 0.5f, 0.0f };
    //float color[] = { 1.0f, 0f, 0f, 1.0f };
    public int loadShader(int type, String shaderCode){
        // 创建着色器，根据type判断是顶点着色器还是片元着色器
        int shader = GLES20.glCreateShader(type);
        // 将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /*
     * 当surface创立时调用，可以用于初始化
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 将背景设置为灰色
        GLES20.glClearColor(0.5f,0.5f,0.5f,1.0f);

        // CPU--->GPU  是通过ByteBuffer 实现数据从cpu到gpu的转换
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());  // GPU重新整理内存

        // 在GPU中，还需要将ByteBuffer转换为FloatBuffer，用以传入OpenGL ES程序
        vertexBuffer = byteBuf.asFloatBuffer();
        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);

        // 1.创建一个顶点着色器  2.将提前写好的顶点着色器从CPU传入GPU  3.编译顶点着色器
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glCompileShader(vertexShader);

        // 1.创建一个片元着色器  2.将提前写好的片元着色器从CPU传入GPU  3.编译片元着色器
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShader);

        //  int vertexShader =  loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode);
        //  int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode);

        // 创建渲染程序  顶点程序、片元程序，本质上都是一个在GPU中运行的可执行程序（OpenGLES程序）
        mProgram = GLES20.glCreateProgram();
        // 将顶点、片元着色器添加到渲染程序中
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        // 链接顶点、片元着色器，并且生成一个OpenGLES程序：mProgram
        GLES20.glLinkProgram(mProgram);
    }

    /*
     * 当窗口大小变化时调用
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    /*
     * 调用以绘制当前帧，也就是显示内容的呈现在这里实现
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // 将上面生成的程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);

        // 获取mProgram中的 vPosition 句柄
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // 启用顶点属性  vPosition 能够允许 cpu 往 gpu写
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // 起始位置在缓冲区的偏移量
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        // 获取mProgram中的 vColor 句柄
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        // 设置颜色
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        // 绘制形状
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
