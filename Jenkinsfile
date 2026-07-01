pipeline {
    agent any

    // 使用 Jenkins 中预配置的 Java 25 与 Maven 工具。
    tools {
        jdk 'jdk-25'
        maven 'maven'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
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
                branch 'main'
            }
            steps {
                // 仅 main 分支执行部署脚本。
                sh 'bash deploy/deploy.sh'
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
