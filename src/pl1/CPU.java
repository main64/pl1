package pl1;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CPU {
	private String difficulty;	//難易度
	private int[][] wb = new int[8][8];	//重み付け盤面
	private String color;	//先手後手情報
	private int[] move = new int[2];	//指し手情報
	private final int ENDSTART_N = 55;	//"ふつう"において終盤完全読みを開始するターン数
	private final int ENDSTART = 49;	//"やさしい"と"難しい"において終盤完全読みを開始するターン数
	
	/*コンストラクタ*/
	public CPU(String difficulty, String PlayerColor){
		setDifficulty(difficulty);
		setCPUColor(PlayerColor);
		evaluate_init();
	}
	
	/*難易度をセット*/
	private void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}
	
	/*難易度を取得*/
	public String getDifficulty() {
		return difficulty;
	}
	
	/*先手後手情報をセット*/
	private void setCPUColor(String PlayerColor) {
		try {
			if(PlayerColor == "white") {
				this.color = "black";
			}else if(PlayerColor == "black") {
				this.color = "white";
			}else {
				throw new ColorException("先手後手情報を設定できません");
			}
		}catch(ColorException ce){
			System.out.println(ce.getMessage());
		}
	}
	
	/*先手後手情報を取得*/
	public String getCPUColor() {
		return color;
	}
	
	/*指し手情報を初期化*/
	private void setMove() {
		move[0] = -1;
		move[1] = -1;	//move[-1, -1]の時、CPUがパスをしたとする
	}
	
	/*盤面評価関数の設定*/
	private void evaluate_init() {
		int i=0, j=0;
		
		/*csvファイルから評価値を読み取って2次元配列wbに格納*/
		/*csvファイルには盤面の左上1/4部分のみ記載*/
		try {
			File f = new File("Othello_wb.csv");
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s;
			while((s = br.readLine()) != null) {
				j = 0;
				String[] data = s.split(",");
				for(String elem : data) {
					wb[i][j] = Integer.parseInt(elem);
					j++;
				}
				i++;
			}
			br.close();
		}catch(IOException e) {
			System.out.println(e);
		}
		
		/*csvファイルから読み取った盤面を反転させて8×8のwbを完成させる*/
		for(i=0; i<4; i++) {
			for(j=4; j<8; j++) {
				wb[i][j] = wb[i][7-j];
			}
		}
		for(i=4; i<8; i++) {
			for(j=0; j<4; j++) {
				wb[i][j] = wb[7-i][j];
			}
		}
		for(i=4; i<8; i++) {
			for(j=4; j<8; j++) {
				wb[i][j] = wb[7-i][7-j];
			}
		}
	}
	
	/*評価値の計算*/
	/*（評価値）=
	   (CPUの石が置かれているところのマスの重みの合計)
	   - (Playerの石が置かれているところのマスの重みの合計)*/
	private int eval(int[][] sb) {
		int e = 0;	//評価値
		int cpu_set;	//CPUが黒ならば4, 白ならば5が入る
		
		/*黒と白どちらの評価値を求めるかを決める*/
		if(getCPUColor() == "black") {
			cpu_set = 4;
		}else {
			cpu_set = 5;
		}
		
		/*cの値に対応する方の評価値を求める*/
		for(int i=0; i<sb.length; i++) {
			for(int j=0; j<sb.length; j++) {
				if(sb[i][j] == cpu_set) {
					e = e + wb[i][j];
				}else if(sb[i][j] == 9-cpu_set) {
					e = e - wb[i][j];
				}
			}
		}
		
		return e;
	}
	
	/*CPU側の石の数のカウント*/
	private int countDisc(int[][] sb) {
		int num = 0;	//CPU側の石の数
		int cpu_set;	//CPUガ黒ならば4，白ならば5が入る
		
		/*黒と白どちらの数を数えるかを決める*/
		if(getCPUColor() == "black") {
			cpu_set = 4;
		}else {
			cpu_set = 5;
		}
		
		/*cの値に対応する方の数を数える*/
		for(int i=0; i<sb.length; i++) {
			for(int j=0; j<sb.length; j++) {
				if(sb[i][j] == cpu_set) {
					num++;
				}
			}
		}
		
		return num;
	}
	
	/*αβ法による探索
	  depth:現在の深さ, maxdepth:最大の深さ, sb:探索用仮盤面, c:色(1: 黒, 2: 白)
	  end: (終盤の完全探索をするかどうか)
	  alpha: 自分が取りうる評価値の最小, beta: 相手が取りうる評価値の最大
	 */
	private int search(int depth, int maxdepth, int[][] sb, int c, boolean end, int alpha, int beta) {		
		/*葉に到達した時*/
		if(depth == 0) {
			if(end) {
				return countDisc(sb);	//終盤なら石の数を返す
			}else {
				return eval(sb);	//終盤でないなら評価値を返す
			}
		}
		
		int[][] fb = new int[8][8];	//探索の際に利用するコピー先盤面
		int count = 0;	//現在のノードから何通りの手があるかをカウント
				
		/*再帰アルゴリズムによるαβ探索*/
		for(int i=0; i<sb.length; i++) {
			for(int j=0; j<sb.length; j++) {
				/*置ける場所が見つかった場合、探索*/
				if(sb[i][j] == c | sb[i][j] == 3) {
					count++;
					/*探索用盤面に現在の盤面状況をコピー*/
					for(int k=0; k<sb.length; k++) {
						fb[k] = sb[k].clone();
					}
					Othello.update(fb, i, j, c);	//探索用盤面の状況を更新
					int value = search(depth-1, maxdepth, fb, 3-c, end, alpha, beta);	//再帰呼び出し
					if((maxdepth - depth) % 2 == 1) {	//相手側が選択する手を考えるとき
						if(value < beta) {
							beta = value;	//相手は評価値が低くなる手を取る
						}
						if(getDifficulty() != "やさしい") {
							if(alpha >= beta) {	//今のノードの評価値は兄弟ノードよりも低くなることが確定している
								return beta;	//αカット
							}
						}
					}else {	//自分側が選択する手を考えるとき
						if(getDifficulty() != "やさしい") {	//難易度normal以上の時
							if(value > alpha) {
								alpha = value;	//自分は評価値が高くなる手を取る
								/*もしゲーム木の根にいる場合、暫定の指し手を保存*/
								if(depth == maxdepth) {
									move[0] = i;
									move[1] = j;
								}
							}
							if(alpha >= beta) {	//今のノードの評価値は兄弟ノードよりも高くなることが確定している
								return alpha;	//βカット
							}
						}else {	//難易度easyの時
							if(value < alpha) {
								alpha = value;	//自分も評価値が低くなる手を取る
								/*もしゲーム木の根にいる場合、暫定の指し手を保存*/
								if(depth == maxdepth) {
									move[0] = i;
									move[1] = j;
								}
							}
						}
					}
				}
			}
		}
		
		/*パスではないが、途中で置く場所がなくなった場合の処理*/
		if(depth != maxdepth & count == 0) {
			if(!Othello.passCheck(sb, 3 - c)) {	//どちらも打つ手がなくなった(対局終了)ならば
				return eval(sb);	//その時点での評価値を返す
			}else {	//そうでないなら
				return search(depth-1, maxdepth, sb, 3-c, end, alpha, beta);	//手を打たずに相手に番を返す
			}
		}
		
		/*自分の手番ならα値, 相手の手番ならβ値を返す*/
		if((maxdepth - depth) % 2 == 1) {
			return beta;
		}else {
			return alpha;
		}
	}
	
	/*難易度易*/
	private void easy(int[][] board, int c) {
		int depth = 1;	//何手先まで読むか
		if(Client.getTurn() < ENDSTART - depth + 1) {
			search(depth, depth, board, c, false, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}else {	//完全探索
			int enddepth = 60 - Client.getTurn() + 1;
			search(enddepth, enddepth, board, c, true, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
	}
	
	/*難易度中*/
	private void normal(int[][] board, int c) {
		int depth = 2;
		if(Client.getTurn() < ENDSTART_N - depth + 1) {
			search(depth, depth, board, c, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}else if(Client.getTurn() < ENDSTART_N) {	//完全探索が始まる前の手番までの先読みをする
			int toend = ENDSTART_N - Client.getTurn();
			search(toend, toend, board, c, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}else {
			int enddepth = 60 - Client.getTurn() + 1;
			search(enddepth, enddepth, board, c, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
	}
	
	/*難易度高*/
	private void difficult(int[][] board, int c) {
		int depth = 8;
		if(Client.getTurn() < ENDSTART - depth + 1) {
			search(depth, depth, board, c, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}else if(Client.getTurn() < ENDSTART) {	//完全探索が始まる前の手番までの先読みをする
			int toend = ENDSTART - Client.getTurn();
			search(toend, toend, board, c, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}else {
			int enddepth = 60 - Client.getTurn() + 1;
			search(enddepth, enddepth, board, c, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
	}
	
	/*CPUクラスのメイン関数のようなもの*/
	public void CPUMain(int[][] board) {	
		int c;	//CPUが黒ならば1, CPUが白ならば2が入る
		
		/*cに入れる値を決める*/
		if(getCPUColor() == "black") {
			c = 1;
		}else {
			c = 2;
		}
		
		setMove();
		
		/*探索方法を場合分け*/
		try{
			if(difficulty == "やさしい") {
				easy(board, c);
			}else if(difficulty == "ふつう") {
				normal(board, c);
			}else if(difficulty == "難しい") {
				difficult(board, c);
			}else {
				throw new DifficultyException("難易度が正しくありません");
			}
		}catch(DifficultyException de) {
			System.out.println(de.getMessage());
		}		
	}
	
	/*指し手を取得*/
	public int[] getMove() {
		return move;
	}
}


/*難易度が正しく設定されていないときの例外クラス */
class DifficultyException extends Exception{
	DifficultyException(String msg){
		super(msg);
	}
}


/*先手情報が正しく設定できないときの例外クラス*/
class ColorException extends Exception{
	ColorException(String msg){
		super(msg);
	}
}