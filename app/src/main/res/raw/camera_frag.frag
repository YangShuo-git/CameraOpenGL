// 使用采样器，必须有此行
#extension GL_OES_EGL_image_external : require

// 所有float类型数据的精度是lowp，但是SurfaceTexture 比较特殊
precision mediump float;
varying vec2 aCoord;

// 这是一种采样器，用于访问 Android 通过 SurfaceTexture 提供的OES纹理（通常来自摄像头预览或视频解码器）
uniform samplerExternalOES vTexture;

void main(){
     // texture2D是Opengl自带的函数
//     vec4 rgba = texture2D(vTexture, aCoord);
//     gl_FragColor=vec4(rgba.r, rgba.g, rgba.b, rgba.a);

     // 1.黑白滤镜
//      vec4 rgba = texture2D(vTexture,aCoord);
//      float color=(rgba.r+rgba.g+rgba.b) / 3.0;
//      gl_FragColor=vec4(color,color,color,1);
     // 2.电影滤镜
      vec4 rgba = texture2D(vTexture,aCoord);
      gl_FragColor=rgba+vec4(0.3,0.3,0.0,0.0);
     // 3.逆时针旋转90°
//      vec4 rgba = texture2D(vTexture,vec2(1.0-aCoord.y,aCoord.x));
//      gl_FragColor=vec4(rgba.r,rgba.g,rgba.b,rgba.a);
}
