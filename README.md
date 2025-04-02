apply plugin: 'maven-publish'


//def releasesRepoUrl = 'http://xxx.xx.com/artifactory/repository/release/'
//def snapshotsRepoUrl =  'http://xxx.xx.com/artifactory/repository/snapshot/'
//           // 基于版本名称选择不同的仓库地址
def url1="https://repo1.maven.org/maven2/"
def url1_debug=url1+"snapshot"
def url2="https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
def url2_debug=url2+"snapshot"

def releasesRepoUrl = url2
def snapshotsRepoUrl = url2_debug
//https://repo1.maven.org/maven2/ 仓库1
//https://repo1.maven.org/maven2/snapshot 仓库2

def user_name = "A3fFnUEe"
def pwd = "zLe85E3K4atAPebK7DOGIw4PNt3oiFsL8IG7bwnCArS4"
user_name="452693688@qq.com"
user_name="guom"
pwd="gm@198712121411"
def aar_version="1.9.9"
def group_id="com.retrofits.net"
def artifact_id='retrofit'
//证书后八位
//signing.keyId=C4693B36
def keyId="E7DD865A"
// 备份私钥保存的文件路径
//signing.secretKeyRingFile=E\:\\Android\\0xC4693B36_SECRET.gpg
def secretKeyRingFile="D:\\aar\\guom_0xE7DD865A_SECRET.gpg"
//
//注册sonatype的用户名与密码
def ossrhUsername=user_name
def ossrhPassword=pwd

afterEvaluate {
publishing {
//配置maven 仓库
repositories { RepositoryHandler handler ->
handler.maven { MavenArtifactRepository mavenArtifactRepository ->
allowInsecureProtocol(true)//允许明文传输
//url 必须配置
url = version.endsWith('SNAPSHOT') ? snapshotsRepoUrl : releasesRepoUrl
credentials {
username user_name
password pwd
}
}
//本地maven仓
// handler.mavenLocal()  // 发布到默认的 本地maven 仓库 ，路径： USER_HOME/.m2/repository/
// 仓库用户名密码
// handler.maven { MavenArtifactRepository mavenArtifactRepository ->
//     // maven 仓库地址
//     url 'http://10.0.192.56:8081/repository/core/'
//     // 访问仓库的 账号和密码
//     credentials {
//         username = "meiTest"
//         password = "123456"
//     }
// }
}

        //配置发布产物
        publications { PublicationContainer publicationContainer ->
            //发布 snapshot 包
           /* debug(MavenPublication) {
                //过依赖生成 aar 包任务
                afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
                // 也可以指定上传的AAR包，但是需要先手动生成aar
                // artifact "$buildDir/outputs/aar/${project.name}-debug.aar"
                from components.debug//  使用 Android Gradle 插件生成的组件，作为发布的内容
                // 增加上传源码的 task
                artifact sourceJar
                groupId = group_id
                //一般是项目名或者模块名
                artifactId = artifact_id
                version  = aar_version

            }*/

            releaseAAR(MavenPublication) {
                //过依赖生成 aar 包任务
                //afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
                // 也可以指定上传的AAR包，但是需要先手动生成aar
                //artifact "$buildDir/outputs/aar/${project.name}-release.aar"
                from components.release//  使用 Android Gradle 插件生成的组件，作为发布的内容
                // 增加上传源码的 task
                //artifact sourceJar
                //GroupId和ArtifactId被统称为“坐标”是为了保证项目唯一性而提出的，
                //如果要把你项目弄到maven本地仓库去，你想要找到你的项目就必须根据这两个id去查找
                groupId = group_id
                //一般是项目名或者模块名。
                artifactId = artifact_id
                version  = aar_version

            }
        }
    }
}


//增加上传源码的task
task sourceJar(type:Jar){
from android.sourceSets.main.java.srcDirs
archiveClassifier = "source"
}

