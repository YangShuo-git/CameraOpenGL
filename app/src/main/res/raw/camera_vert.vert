// 把顶点坐标给该变量，确定要画画的形状;  4个数组矩阵
attribute vec4 vPosition;
// 把纹理坐标给该变量，接收采样器采样图片的坐标
attribute vec4 vCoord;
// 变换矩阵，需要将原本的vCoord 与 该矩阵相乘，才能得到 SurfaceTexure 的正确的采样坐标
uniform mat4 vMatrix;

// 像素点  传给片元着色器，在frag程序中有同名变量
varying vec2 aCoord;

void main(){
    // 将vPosition传给gl_Position，通知gpu需要渲染成什么样的形状
    gl_Position=vPosition;
    // 用来给片元上色，必须是矩阵*纹理坐标
    aCoord= (vMatrix * vCoord).xy;
}
