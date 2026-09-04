// bgssai-short 部署流水线（Jenkins）。
//
// 部署通道唯一：本流水线是该产品的唯一部署入口，仅手动触发。
// 构建与 ship 的全部逻辑收敛在中央仓 bgssai-workflows 的 Jenkins 共享库（vars/ + resources/），
// 本文件只声明参数与阶段，避免多个产品仓各自维护一份实现而漂移。
//
// **目标环境由 Job 名前缀决定**（内部名 dev-short-deploy / prod-short-deploy，界面显示名
// bgssai-short deploy(dev) / bgssai-short deploy(prod)），
// 刻意不提供 environment 下拉框 —— 下拉框选错就会把生产环境当开发环境部署，
// 而 Job 名会出现在面包屑、构建页与通知里，看错的机会小得多。详见共享库 bgssaiResolveEnvironment。
//
// 触发方式：仅手动，点 Build 即执行（无需填写任何确认串）。部署失败时不自动重试、不自动重新部署，
// 仅报告失败并定位原因，重新部署由人工手动执行。
//
// 真正的远端部署仍由本仓 deploy/remote-deploy.sh 承担（目标机自建时直接调用源码树里那一份），
// 须与中央仓 bgssai-workflows 的 deploy/ 逐字节一致。
//
// **dev / prod 都走「目标机自建」**：控制器不构建 fat jar、也不推 jar，由目标机自己 git 拉最新
// 代码、就地构建、就地部署。走哪条由共享库 bgssaiDeployEnd 按环境判断。
// 目标机因此需要具备构建工具链，一次性准备见中央仓 jenkins/install/provision-build-host.sh。
//
// 本产品 admin 与 Jenkins 控制器共机（Jenkins 占 8080）。两端应用现统一 8080 HTTP；部署 short-admin 前须确认本机 8080 不被 Jenkins 占用，或把 short-admin 迁走。

@Library('bgssai') _

// 本产品的构建口径只声明一次，两条部署通道共用：
//   dev / prod -> bgssaiRemoteBuild：目标机自己 git 拉代码、就地构建、就地部署
//   test       -> bgssaiShip：控制器构建 fat jar 后推到目标机
// 「哪个环境走哪条」只写在共享库 bgssaiDeployEnd 里一处，不在各产品仓各写一遍 if。
//
// 刻意不写 def：声明式流水线里，pipeline 块外用 def 定义的变量只是脚本方法的局部量，
// steps / when 块未必看得见（视 Jenkins 版本而定，症状是运行期报 No such property）。
// 不加 def 则落进脚本 binding，各阶段一律可见。
PRODUCT = [
  // 产品键 short：仓 bgssai-short、主机清单 SHORT_*_HOST、
  // 目标机目录 / 服务名 bgssai-short-{user,admin}。
  product: 'short',
  packageManager: 'npm',
  frontendScript: 'build:deploy',
  appPort: '8080',
  healthScheme: 'http',
]

pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    // 180 而非原来的 60：目标机自建把构建挪到了目标机，两端顺序部署时墙钟由目标机的构建速度
    // 决定，首次部署还要在那台机器上冷启 ~/.m2 与 npm 缓存，单端点 20 分钟以上属正常
    // （之后的增量部署快得多）。这是上限不是开销：单个端点的构建另由 remote-build.sh 的
    // BGSSAI_BUILD_TIMEOUT_SECONDS（默认 2700s）单独封顶，卡死的构建不会一直拖到这里。
    timeout(time: 180, unit: 'MINUTES')
  }

  parameters {
    choice(name: 'target', choices: ['both', 'user', 'admin'], description: '部署目标（both=两端 / user=仅用户端 / admin=仅管理端），不改就是 both')
  }

  stages {
    stage('Resolve environment') {
      steps {
        script {
          env.BGSSAI_ENV = bgssaiResolveEnvironment(action: '部署', requireConfirm: false)
        }
      }
    }

    stage('Build') {
      // dev / prod 整段跳过：目标机会自己拉代码、自己打 jar，控制器这一步没有任何产物要产出。
      when { expression { !(env.BGSSAI_ENV in ['dev', 'prod']) } }
      steps {
        bgssaiBuildJars(PRODUCT)
      }
    }

    stage('Deploy user') {
      when { expression { params.target in ['both', 'user'] } }
      steps {
        bgssaiDeployEnd(PRODUCT + [end: 'user', environment: env.BGSSAI_ENV])
      }
    }

    stage('Deploy admin') {
      when { expression { params.target in ['both', 'admin'] } }
      steps {
        bgssaiDeployEnd(PRODUCT + [end: 'admin', environment: env.BGSSAI_ENV])
      }
    }
  }

  post {
    success {
      echo "部署成功: bgssai-short environment=${env.BGSSAI_ENV} target=${params.target}"
    }
    failure {
      echo "部署失败: bgssai-short environment=${env.BGSSAI_ENV} target=${params.target}"
      echo '按仓库约定：不自动重跑本流水线、不自动重新部署。请先定位原因，再由人工手动触发。'
      echo '目标机自建构建失败时远端未被改动；健康检查失败时 remote-deploy.sh 已自动回滚到上一个可用 jar，服务应仍在跑旧版本。'
    }
  }
}
