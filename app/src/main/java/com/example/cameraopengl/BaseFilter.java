package com.example.cameraopengl;


import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BaseFilter {
    private Context mContext;
    protected int mVertexShaderId;
    protected int mFragShaderId;
    protected final FloatBuffer mGlVertexBuffer;
    protected final FloatBuffer mGlTextureBuffer;
    protected String mVertexShader;
    protected String mFragShader;
    protected int mProgramId;
    protected int vTexture;
    protected int vMatrix;
    protected int vPosition;
    protected int vCoord;
    protected int mOutputHeight;
    protected int mOutputWidth;
    protected int y;
    protected int x;

    public BaseFilter(Context context, int vertexShaderId, int fragShaderId) {
        mContext = context;
        mVertexShaderId = vertexShaderId;
        mFragShaderId   = fragShaderId;

        //创建顶点缓冲区
        mGlVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGlVertexBuffer.clear();
        float[] VERTEXT = {
                -1.0f, 1.0f,
                1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f
        };
        mGlVertexBuffer.put(VERTEXT);

        //创建纹理坐标缓冲区
        mGlTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGlTextureBuffer.clear();
        float[] TEXTURE = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        };
        mGlTextureBuffer.put(TEXTURE);

        init(mContext);
        resetCoordinate();
    }

    private void init(Context mContext) {
        //OpenGL ES渲染管线的初始化阶段，负责加载、编译着色器程序
        // 为后续的图像处理操作准备必要的OpenGL资源。
        //读取着色器信息
        mVertexShader = OpenGLUtils.readRawShaderFile(mContext, mVertexShaderId);
        mFragShader   = OpenGLUtils.readRawShaderFile(mContext, mFragShaderId);
        //创建着色器程序
        mProgramId = OpenGLUtils.loadProgram(mVertexShader, mFragShader);
        //获取着色器变量，需要赋值
        vPosition = GLES20.glGetAttribLocation(mProgramId, "vPosition");
        vCoord    = GLES20.glGetAttribLocation(mProgramId, "vCoord");
        vMatrix   = GLES20.glGetUniformLocation(mProgramId, "vMatrix");
        vTexture  = GLES20.glGetUniformLocation(mProgramId, "vTexture");
    }

    public void prepare(int width, int height, int x, int y) {
        mOutputWidth = width;
        mOutputHeight = height;
        this.x = x;
        this.y = y;
    }

    public int onDrawFrame(int textureId) {
        GLES20.glViewport(x, y, mOutputWidth, mOutputHeight);

        GLES20.glUseProgram(mProgramId);

        //设置顶点属性数组
        mGlVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGlVertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);
        //设置纹理坐标数组
        mGlTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGlTextureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);

        //绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //通知着色器：采样器vTexture应该从纹理单元0读取数据
        GLES20.glUniform1i(vTexture, 0);
        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        return textureId;
    }

    public void release() {
        GLES20.glDeleteProgram(mProgramId);
    }

    protected void resetCoordinate() { }
}
