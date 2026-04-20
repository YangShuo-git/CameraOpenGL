package com.example.cameraopengl;


import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

public class Camera2Filter extends BaseFBOFilter {
    protected float[] mMatrix;

    public Camera2Filter(Context mContext, int vertexShaderId, int fragShaderId) {
        super(mContext, vertexShaderId, fragShaderId);
    }

    public Camera2Filter(Context mContext) {
        super(mContext, R.raw.camera_vert, R.raw.camera_frag);
    }

    @Override
    protected void resetCoordinate() { }

    @Override
    public int onDrawFrame(int textureId) {
        //锁定绘制的区域  绘制是从左下角开始的
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        //绑定FBO，后续渲染将输出到FBO纹理
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        //激活着色器程序
        GLES20.glUseProgram(mProgramId);

        mGlVertexBuffer.position(0); //将顶点缓冲区的指针重置到起始位置
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGlVertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

        mGlTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGlTextureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);

        //将4x4变换矩阵传递给着色器，用于处理相机预览的旋转和镜像
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMatrix, 0);

        //SurfaceTexture 对应 GL_TEXTURE_EXTERNAL_OES 类型
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        //通知着色器：采样器vTexture应该从纹理单元0读取数据
        GLES20.glUniform1i(vTexture, 0);

        //绘制到FBO纹理
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 解绑FBO（渲染已完成，数据已保存在FBO纹理中）
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        // 返回FBO纹理ID，供后续使用
        return mFBOTextures[0];
    }

    public void setMatrix(float[] matrix) {
        mMatrix = matrix;
    }
}

//解绑FBO只是切换渲染目标，不会影响已经渲染到FBO纹理中的数据。
//FBO纹理中的数据会一直保留，直到被新的渲染操作覆盖或纹理被删除
