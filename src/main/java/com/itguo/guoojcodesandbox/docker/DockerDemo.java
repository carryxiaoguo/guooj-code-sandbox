package com.itguo.guoojcodesandbox.docker;


import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerDemo {

    public static void main(String[] args) throws InterruptedException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")  // 使用 Unix socket 连接
                .build();
        DockerClientBuilder clientBuilder = DockerClientBuilder.getInstance(config);


        //PingCmd pingCmd = clientBuilder.build().pingCmd();
        String images = "nginx:latest";
        PullImageCmd pullImageCmd = clientBuilder.build().pullImageCmd(images);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像"+item.getStatus());
                super.onNext(item);
            }
        };
        pullImageCmd
                .exec(pullImageResultCallback)
                .awaitCompletion();
        System.out.println("镜像准备好了!");
    }
}
