package com.example.cameraopengl;


import android.content.Context;
import android.opengl.GLES20;


public class BaseFBOFilter extends BaseFilter {

    protected int[] mFrameBuffers;
    protected int[] mFBOTextures;
    public BaseFBOFilter(Context mContext, int mVertexShaderId, int mFragShaderId) {
        super(mContext, mVertexShaderId, mFragShaderId);
    }

    @Override
    public void prepare(int width, int height,int x,int y) {
        super.prepare(width, height, x, y);

        loadFBO();
    }

    private void loadFBO() {
        if (mFrameBuffers != null) {
            destroyFrameBuffers();
        }

        //创建并配置一个空的 2D 纹理，为后续FBO的颜色附件做准备
        mFBOTextures = new int[1];
        OpenGLUtils.genTextures(mFBOTextures);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFBOTextures[0]);
        //pixels: null 表示仅分配 GPU 内存，不填充初始数据（离屏渲染的纹理不需要 CPU 端初始内容）
        //为该纹理创建一个空白的GPU内存，后续渲染内容将写入此处
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mOutputWidth, mOutputHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //创建FBO
        mFrameBuffers = new int[1];
        GLES20.glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
        //将之前创建的纹理作为颜色附件“挂载” FBO 上
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFBOTextures[0], 0);

        //解绑纹理和FBO，避免影响后续操作
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //后续使用时，只需调用 glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0])
        //即可将渲染目标切换为该离屏纹理，渲染完成后再绑定回 0 即可切回屏幕。
        //离屏渲染实际上还是在纹理中渲染，这个纹理需要作为颜色附件挂载到 FBO 上
    }

    @Override
    protected void resetCoordinate() {
        mGlTextureBuffer.clear();
        float[] TEXTURE = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };
        mGlTextureBuffer.put(TEXTURE);
    }

    public void destroyFrameBuffers() {
        //删除fbo的纹理
        if (mFBOTextures != null) {
            GLES20.glDeleteTextures(1, mFBOTextures, 0);
            mFBOTextures = null;
        }
        //删除fbo
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }
}

