# Sequential SSH Commands Rundeck Plugin

> ⚠ このプロジェクトは [jsboak/sequential-commands-plugin](https://github.com/jsboak/sequential-commands-plugin) をフォークしたものです。  
> 元プロジェクトのライセンス（Apache License 2.0）に従って改変・再配布しています。

このプラグインは、Rundeck における **Node Step Job** プラグインであり、リモートノードに対して **1つのSSHセッションで複数のコマンドを順番に送信**できます。

同じSSHチャネル内でコマンドを実行するため、**状態（カレントディレクトリや環境変数）を共有したまま複数のコマンド**を実行することができます。  
ネットワーク機器や、特定のディレクトリからでないと動かせないコマンドがあるシステムに最適です。

![Screen Shot 2021-12-30 at 3 55 39 PM](https://user-images.githubusercontent.com/11511251/147795129-b5a593ec-82e8-4acd-a25b-69270fd8c55a.png)

## ビルド方法
Java 1.8 環境で以下のコマンドを実行してください

`gradle build`
生成されたJARファイルは build/libs/ フォルダ内に出力されます。


## インストール方法
生成された sequential-commands-plugin-x.y.z.jar を Rundeck の次のディレクトリへコピーしてください：
$RDECK_BASE/libext/
GUIからアップロードすることも可能です。詳しくは Rundeck公式ドキュメントの こちら を参照してください。

## 使用方法
このプラグインは、Rundeckでノードに設定されたSSH認証情報を使用します。

To add a command to the Job Step, click on **`Add Custom Field`**:

![Screen Shot 2022-03-16 at 11 03 31 AM](https://user-images.githubusercontent.com/11511251/158657441-0dc90855-fe4f-461b-a20b-9d5a1968ade6.png)

In the configuration popup, set the field `Field Label` and `Field Key`. Inputs here **do not** influence the command that is sent to the remote node - they serve to identify the commands within the Job Step.  Optionally set the `Description` field:

![Screen Shot 2022-03-16 at 11 10 58 AM](https://user-images.githubusercontent.com/11511251/158658778-aa5636a3-1c84-4c5f-a8a4-3e8e3cbe5c07.png)

This then adds a field within the Job Step where you can input a command that you want to send to the remote node. Continue to add _Custom Fields_ to the Job step such that you can send multiple commands to the node in a single SSH channel. Commands are sent to the remote node in sequential order. 

## Example
As an example, in the screenshot below, the order of commands send to the remote node would be: `enable`, then a secure-job-option containing the _enable password_ is sent to the device (this will not be shown in the logs output), this is then followed by the `terminal length 0` command and then finally the `show interfaces` command. 
You can view and download this example job [here](https://github.com/jsboak/sequential-commands-plugin/blob/main/example-jobs/Cisco_Router_-_Show_Interfaces.yaml).

![Screen Shot 2022-03-16 at 11 20 26 AM](https://user-images.githubusercontent.com/11511251/158660331-b6771155-8765-44bd-a752-53e31ec825cb.png)

The output for this example (on a Cisco CSR) would appear like so:

![Screen Shot 2022-03-16 at 11 27 20 AM](https://user-images.githubusercontent.com/11511251/158661436-391f4134-b96d-4606-898f-34938d9ccad1.png)

Optionally select the **`Strict Host Key Checking`** checkbox to choose whether or not Rundeck checks that the remote-node is in the `known_hosts` file.
