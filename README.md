
更新日時　5/14 2:22

# 1.実装要件
## Client
# クラス(画面遷移)

## Clientクラス(サーバ通信)
## CPUクラス
## Othelloクラス
## Serverクラス

# 2.実装メソッド及び変数
## Clientクラス(画面遷移)
Client コンストラクタ
chooseMode ローカル対局かネットワーク対局か選ばせる
setName ネットワーク対戦時にプレイヤの名前を設定させる
waitMode 通信対局待ち
chooseLevel コンピュータ対戦時にレベルを選ばせる
String getTurnText paintGameにおいて画面に手番の表示をするためにどちらのターンか選ばせる
int getTurn 現在のターン数を返す
paintGame 対局開始時に盤面を描画
reflectMap 盤面の情報を更新
class BoardButton 何番目かの整数iを保持
paintResult 対局終了時に結果を出力
actionPerfomed　一部のボタンの挙動を定義
## Clientクラス(サーバ通信)
割愛
## CPUクラス
コンストラクタ
setDifficluty (String 難易度)
setCPUColor(String プレイヤの色)
割愛
## Othelloクラス
putDisk(int y,int x) 石を置く
pass パス操作
getBlack 黒の意思の数
getWhite 白の意思の数
boolean passCheck(int board[][],int c) パスすべきか否か パスならばfalse
int [][] getBoard 盤面を返す
int getGridState(int y,int x)盤面のマスの状態を返す
String judge() 勝敗判定　勝者("draw"/"black"/"white")
int endCheck 0:非終了　1:終了
int [][] update (int board[][],int y,int x,int color) 盤面に石を置き計算して返すメソッド
割愛
## Serverクラス
割愛

# 3.フロー

# 4.未実装内容及び未決定事項
## Clientクラス(画面遷移)

## Clientクラス(サーバ通信)
## CPUクラス
## Othelloクラス
## Serverクラス

# 2.実装メソッド及び変数
## Clientクラス(画面遷移)
## Clientクラス(サーバ通信)
## CPUクラス
## Othelloクラス
## Serverクラス

# 3.フロー

# 4.未実装内容及び未決定事項
