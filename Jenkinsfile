pipeline {
    agent any
    options {
        timeout(time: 1, unit: "HOURS")
    }
    environment {
        USER_NAME = 'Jenkins'
        // 限制 Maven JVM 堆内存，避免在低配服务器(4核4G)上构建时内存压力过大
        MAVEN_OPTS = '-Xmx1024m -XX:MaxMetaspaceSize=256m'
        // Jenkins 构建使用 1 个 Maven 线程，并关闭 javac verbose 日志，降低内存和日志压力
        MAVEN_CI_ARGS = '-B --no-transfer-progress -T 1 -Dmaven.compiler.verbose=false'
    }
    tools {
        jdk "jdk25"
    }

    stages {
        stage('Checkout') {
            steps {
                // 检出仓库源码。
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // 构建可部署 Jar，测试在独立阶段执行。
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // 执行 JUnit 5 测试并生成 JaCoCo 覆盖率数据。
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    jacoco execPattern: 'target/jacoco.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java'
                }
            }
        }

        stage('Package') {
            steps {
                // 归档 EcoVault 应用 Jar。
                archiveArtifacts artifacts: 'target/ecovault.jar', fingerprint: true, onlyIfSuccessful: true
            }
        }

        stage('Deploy') {
            when {
                branch 'master'
            }
            steps {
                // 仅 master 分支执行部署脚本。
                //sh 'bash deploy/deploy.sh'
            }
        }
    }

    post {
        success {
            echo 'EcoVault 流水线执行成功。'
        }
        failure {
            echo 'EcoVault 流水线执行失败，请检查构建日志。'
        }
    }
}
