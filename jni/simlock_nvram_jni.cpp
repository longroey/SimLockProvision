#include <jni.h>
#include "com_reacheng_simlock_provision_NvRamNative.h"
#include "libnvram.h"
#include "libfile_op.h"
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include "Custom_NvRam_LID.h"

/*
类型         相应的签名
boolean        Z
byte           B
char           C
short          S
int            I
long           L
float          F
double         D
void           V
object         L用/分隔包的完整类名：   Ljava/lang/String;
Array          [签名          [I      [Ljava/lang/Object;
Method         (参数1类型签名 参数2类型签名···)返回值类型签名
*/

/*
Java类型      别名　　        本地类型　　                字节(bit)
boolean     jboolean      unsigned char　　          8, unsigned
byte        jbyte         signed char　　　　        8
char        jchar         unsigned short　　         16, unsigned
short       jshort        short　　　                16
int         jint          long　　　　                32
long        jlong         __int64　　　　           64
float       jfloat        float　　　                   32
double      jdouble       double　　　             64
void        void        　　　　                        n/a
Object      _jobject      *jobject
*/

JNIEXPORT jboolean JNICALL Java_com_reacheng_simlock_provision_NvRamNative_writeNv( JNIEnv* env,
                                        jobject thiz )
{
    return FileOp_BackupToBinRegion_All();
}
