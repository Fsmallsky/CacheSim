import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;


public class CCacheSim extends JFrame implements ActionListener{

	private JPanel panelTop, panelLeft, panelRight, panelBottom;
	private JButton execStepBtn, execAllBtn, fileBotton;
	private JComboBox csBox, bsBox, wayBox, replaceBox, prefetchBox, writeBox, allocBox;
	private JFileChooser fileChoose;
	
	private JLabel labelTop,labelLeft,rightLabel,bottomLabel,fileLabel,fileAddrBtn,
		    csLabel, bsLabel, wayLabel, replaceLabel, prefetchLabel, writeLabel, allocLabel;
	private JTextField results[];
	private JLabel stepLabel1, stepLabel2;


    //参数定义
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "直接映象", "2路", "4路", "8路", "16路", "32路" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "不预取", "不命中预取" };
	private String write[] = { "写回法", "写直达法" };
	private String alloc[] = { "按写分配", "不按写分配" };
	//private String typename[] = { "读数据", "写数据", "读指令" };
	//private String hitname[] = {"不命中", "命中" };
	
	//右侧结果显示
	private String rightLable1[]={"访问总次数：","读指令次数：","读数据次数：","写数据次数："};
	private String rightLable2[]={"缺失总次数：","读指令缺失数：","读数据缺失数：","写数据缺失数："};
	private String rightLable3[]={"总缺失率：","读指令缺失率：","读数据缺失率：","写数缺失率："};
	private String outStr[];
	//打开文件
	private File file;
	
	//分别表示左侧几个下拉框所选择的第几项，索引从 0 开始
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	//          总大小 *4          块大小*2            组相连                      LRU/FIFO/RAND    不/预取                      写回/写直达             按/不按 写分配 
	//其它变量定义
	private FileReader file_reader;   //文件字节流
	private BufferedReader in;  //文件中的一行
	private int cache[][];   //cache的数据结构，
	private boolean dirty[];  // cache块的是否为脏的标志
	private int total_num, total_miss, read_data_num, read_data_miss, read_inst_num, 
	            read_inst_miss, write_num, write_miss;  //四种访问次数、缺失数
	private double miss_rate, read_data_miss_rate, read_inst_miss_rate, write_miss_rate;   //缺失代价
	private long cache_num, block_size, cache_size;  //cache中的块数目、每块的字节数、cache的总大小
	private int row;  //cache中的块数目
	
	
	
	/*
	 * 构造函数，绘制模拟器面板
	 */
	public CCacheSim(){
		super("Cache Simulator");
		draw();
	}
	
	
	//响应事件，共有三种事件：
	//   1. 执行到底事件
	//   2. 单步执行事件
	//   3. 文件选择事件
	public void actionPerformed(ActionEvent e){
				
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep();
		}
		if (e.getSource() == fileBotton){
			fileChoose = new JFileChooser("F:\\大三下\\体系结构\\experiments\\lab2\\MyCache\\MyCache模拟器\\地址流");
			int fileOver = fileChoose.showOpenDialog(null);
			if (fileOver == 0) {
				   String path = fileChoose.getSelectedFile().getAbsolutePath();
				   fileAddrBtn.setText(path);
				   file = new File(path);
				   readFile();
				   initCache();
			}
		}
	}
	
	/*
	 * 初始化 Cache 模拟器
	 * cache行空为0,脏为-1，否则为1
	 */
	public void initCache() {
		outStr = new String[6];
	    row = (2048<<(2*csIndex))/(16<<bsIndex);
		cache = new int[row][2];   //value 和    标志
		dirty = new boolean[row];
		for(int i=0; i<row; i++) {
			cache[i][0] = -1;
			cache[i][1] = -1;
			dirty[i] = false;
		}
		
		total_num = 0;
		total_miss = 0;
		read_data_num = 0;
		read_data_miss = 0;
		read_inst_num = 0;
		read_inst_miss = 0;
		write_num = 0;
		write_miss = 0;
		miss_rate = 0;
		read_data_miss_rate = 0;
		read_inst_miss_rate = 0;
		write_miss_rate = 0;
		cache_size = 2048<<(2*csIndex);
		block_size = 16<<bsIndex;
		cache_num = cache_size/block_size/(1<<wayIndex);
		
		//in = new BufferedReader(file_reader);
	}
	
	/*
	 * 将指令和数据流从文件中读入
	 */
	public void readFile() {
		
		try {
			file_reader = new FileReader(file);
			in = new BufferedReader(file_reader);
			//for(int i = 0; i<10; i++)
				//System.out.println(in.readLine());
		}catch(Exception e2) {
			e2.printStackTrace();
		}
	}
	
	/*
	 * 模拟单步执行
	 */
	public void simExecStep() {
		String inst;
		String[] mRateStr = new String[4];
		DecimalFormat df = new DecimalFormat("0.00");
		try {
			inst = in.readLine();
			if(inst != null && inst.length() != 0)
			{
				long addr = hexStrToLong(inst.substring(2));  //将十六进制字符串转化为Long型整数
				execute(addr, inst);
				//下面生成输出的字符串并图形界面输出
				mRateStr[0] = df.format(miss_rate*100)+"%";
				mRateStr[1] = df.format(read_inst_miss_rate*100)+"%";
				mRateStr[2] = df.format(read_inst_miss_rate*100)+"%";
				mRateStr[3] = df.format(write_miss_rate*100)+"%";
				outStr[0] = " "+rightLable1[0]+total_num+"  \t"+rightLable2[0]+total_miss+"    \t"+rightLable3[0]+mRateStr[0];
				outStr[1] = " "+rightLable1[1]+read_inst_num+"  \t"+rightLable2[1]+read_inst_miss+"  \t"+rightLable3[1]+mRateStr[1];
				outStr[2] = " "+rightLable1[2]+read_data_num+"  \t"+rightLable2[2]+read_data_miss+"  \t"+rightLable3[2]+mRateStr[2];
				outStr[3] = " "+rightLable1[3]+write_num+"  \t"+rightLable2[3]+write_miss+"  \t"+rightLable3[3]+mRateStr[3];
				for(int i=0; i<4; i++)
					results[i].setText(outStr[i]);
				stepLabel1.setText(outStr[4]);
				stepLabel2.setText(outStr[5]);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 模拟执行到底
	 */
	public void simExecAll() {
		String inst;
		try {
			for(inst = in.readLine(); inst != null && inst.length() != 0; inst = in.readLine())
			{
				long addr = hexStrToLong(inst.substring(2));
				execute(addr, inst);
//				System.out.println(total_num+"\t"+total_miss+"\t"+miss_rate);
//				System.out.println(read_data_num+"\t"+read_data_miss+"\t"+read_data_miss_rate);
//				System.out.println(read_inst_num+"\t"+read_inst_miss+"\t"+read_inst_miss_rate);
//				System.out.println(write_num+"\t"+write_miss+"\t"+write_miss_rate);
//				System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			}
			String[] mRateStr = new String[4];
			DecimalFormat df = new DecimalFormat("0.00");
			mRateStr[0] = df.format(miss_rate*100)+"%";
			mRateStr[1] = df.format(read_inst_miss_rate*100)+"%";
			mRateStr[2] = df.format(read_inst_miss_rate*100)+"%";
			mRateStr[3] = df.format(write_miss_rate*100)+"%";
			outStr[0] = " "+rightLable1[0]+total_num+"  \t"+rightLable2[0]+total_miss+"    \t"+rightLable3[0]+mRateStr[0];
			outStr[1] = " "+rightLable1[1]+read_inst_num+"  \t"+rightLable2[1]+read_inst_miss+"  \t"+rightLable3[1]+mRateStr[1];
			outStr[2] = " "+rightLable1[2]+read_data_num+"  \t"+rightLable2[2]+read_data_miss+"  \t"+rightLable3[2]+mRateStr[2];
			outStr[3] = " "+rightLable1[3]+write_num+"  \t"+rightLable2[3]+write_miss+"  \t"+rightLable3[3]+mRateStr[3];
			for(int i=0; i<4; i++)
				results[i].setText(outStr[i]);
			outStr[4] = "";
			outStr[5] = "";
			stepLabel1.setText(outStr[4]);
			stepLabel2.setText(outStr[5]);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	public void execute(long addr,String inst)
	{
		int i;
		boolean succ;
		try {
			long memory_num =(long)(addr / block_size);
			int locate = (int)(memory_num % cache_num)*(1<<wayIndex);
			//System.out.println(addr+"\t"+block_size+"\t"+memory_num+"\t"+locate);
			for(i=0,succ=false; i<1<<wayIndex && !succ; i++)
			{
				if(cache[locate + i][0] == (int)(memory_num/cache_num))
				{//hit
					succ = true;
					switch(inst.charAt(0))
					{
						case '0': 
							outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：命中";
							total_num ++;
							read_data_num ++;
					        read_data_miss_rate = (double)read_data_miss/read_data_num;
					        miss_rate = (double)total_miss/total_num;
					        if(replaceIndex == 0)
					        {
					        	for(int j=0; j<1<<wayIndex; j++)
					        		if(cache[locate + j][0] != -1)
					        			cache[locate + j][1]++;
					        	cache[locate + i][1] = 0;
					        }
					        break;
						case '1':  // 发生的是写数据操作
							// 这两个字符串用于步进是结果的输出
							outStr[4] = " "+"访问类型："+"写数据"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：命中";
							total_num ++;
							write_num ++;
							write_miss_rate = (double)write_miss/write_num;
							miss_rate = (double)total_miss/total_num;
							if(replaceIndex == 0)
							{ // 用于作为替换策略的参考值
								for(int j=0; j<1<<wayIndex; j++)
									if(cache[locate + j][0] != -1)
										cache[locate + j][1]++;
								cache[locate + i][1] = 0;
							}
							if(writeIndex == 0)
								dirty[locate + i] = true;
							break;
						case '2': 
							outStr[4] = " "+"访问类型："+"读指令"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：命中";
							total_num ++;
							read_inst_num ++;
			          		read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
			          		miss_rate = (double)total_miss/total_num;
			          		if(replaceIndex == 0)
			          		{
			          			for(int j=0; j<1<<wayIndex; j++)
			          				if(cache[locate + j][0] != -1)
			          					cache[locate +j][1]++;
			          			cache[locate + i][1] = 0;
			          		}
			          		break;
						case '3':
							outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：命中";
							total_num ++;
					        miss_rate = (double)total_miss/total_num;
				            if(inst.charAt(0) == 'd')
				            {
				            	read_data_num ++;
				            	//read_data_miss ++;
				            	read_data_miss_rate = (double)read_data_miss/read_data_num;
				            }
				            else if(inst.charAt(0) == 'i')
				            {
				            	read_inst_num ++;
				            	//read_inst_miss ++;
				            	read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
				            }
				    
					        if(replaceIndex == 0)
					        {
					        	for(int j=0; j<1<<wayIndex; j++)
					        		if(cache[locate + j][0] != -1)
					        			cache[locate + j][1]++;
					        	cache[locate + i][1] = 0;
					        }
					        break;
						default: System.out.println("error1!!");
					}//switch
				}//if
			}//for
			if(!succ)
			{
				for(i=0; i<1<<wayIndex && !succ; i++)
				{
					if(cache[locate + i][0] == -1)
					{//miss 但 cache中用空位，直接读入
						succ = true;
						switch(inst.charAt(0)){
						case '0': 
							outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr;
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：不命中";
							total_num ++;
							read_data_num ++;
				            read_data_miss ++;
				            total_miss ++;
				            read_data_miss_rate = (double)read_data_miss/read_data_num;
				            miss_rate = (double)total_miss/total_num;
				            
				            dirty[locate + i] =  false;
				            cache[locate + i][0] = (int)(memory_num/cache_num);
			          		for(int j=0; j<1<<wayIndex; j++)
			          			if(cache[locate + j][0] != -1)
			          				cache[locate +j][1]++;
			          		cache[locate + i][1] = 0;
			          		if(prefetchIndex == 1)
			          			execute(addr+block_size, "3 data");
				            break;
						case '1': 
							outStr[4] = " "+"访问类型："+"写数据"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：不命中";
							total_num ++;
							write_num ++;
						    write_miss ++;
						    total_miss ++;
						    write_miss_rate = (double)write_miss/write_num;
						    miss_rate = (double)total_miss/total_num;
						  
						    if(allocIndex == 0)
						    {
							    for(int j=0; j<1<<wayIndex; j++)
							    	if(cache[locate + j][0] != -1)
							    		cache[locate + j][1]++;
							    cache[locate + i][1] = 0;
							    if(writeIndex == 0)
							    	dirty[locate + i] = true;
							    cache[locate + i][0] = (int)(memory_num/cache_num);
							    //if(prefetchIndex == 1)
							    	//execute(addr+block_size, "3 data");
						    }
						    break;
						case '2':  
							outStr[4] = " "+"访问类型："+"读指令"+"   "+"地址："+addr+"\n";
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：不命中";
							total_num ++;
							read_inst_num ++;
		          		    read_inst_miss ++;
		          		    total_miss ++;
		          		    read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
		          		    miss_rate = (double)total_miss/total_num;
		          	   	  
		          		    for(int j=0; j<1<<wayIndex; j++)
		          		    	if(cache[locate + j][0] != -1)
		          		    		cache[locate + j][1]++;
		          		    cache[locate + i][1] = 0;
		          		    cache[locate + i][0] = (int)(memory_num/cache_num); // 将块读入 
		          		    dirty[locate + i] = false;
		          		    if(prefetchIndex == 1)  // 是否预取
		          		    	execute(addr+block_size, "3 inst");
		          		    break;
						case '3':
							outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr;
							outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+(locate/(1<<wayIndex))
									     +"   命中情况：不命中";
							total_num ++;
				            total_miss ++;
				            miss_rate = (double)total_miss/total_num;
				            if(inst.charAt(0) == 'd')
				            {
				            	read_data_num ++;
				            	read_data_miss ++;
				            	read_data_miss_rate = (double)read_data_miss/read_data_num;
				            }
				            else if(inst.charAt(0) == 'i')
				            {
				            	read_inst_num ++;
				            	read_inst_miss ++;
				            	read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
				            }
				            
				            dirty[locate + i] =  false;
				            cache[locate + i][0] = (int)(memory_num/cache_num);  // 将此块读入cache
				             // 下面维持替换时需要的参数
			          		for(int j=0; j<1<<wayIndex; j++)
			          			if(cache[locate + j][0] != -1)
			          				cache[locate +j][1]++;
			          		cache[locate + i][1] = 0;
				            break;
						default: System.out.println("error2!!");
						}//switch
					}//if
				}//for
			}//if
			if(!succ)
			{//miss 且需要替换块
				int max = -1;
				int k = locate;   //需要替换的快的位置
			    for(i=0; i<1<<wayIndex; i++)
			    {
			    	if(cache[locate + i][1] >= max)
			    	{
			    		max = cache[locate + i][1];
			    		k = locate + i;
			    	}
			    }
			    if(replaceIndex == 2)
			    {
			    	k =  locate+2;      //产生一个随机数
			    }
			    switch(inst.charAt(0)){
				case '0':
					outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr+"\n";
					outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+k/(1<<wayIndex)
							     +"   命中情况：不命中";
					total_num ++;
					read_data_num ++;
		            read_data_miss ++;
		            total_miss ++;
		            read_data_miss_rate = (double)read_data_miss/read_data_num;
		            miss_rate = (double)total_miss/total_num;
		          
		            dirty[k] = false;
		            cache[k][0] = (int)(memory_num/cache_num);
		            for(int j=0; j<1<<wayIndex; j++)
		            	if(cache[locate + j][0] != -1)
		            		cache[locate + j][1] ++;
		            cache[k][1] = 0;
		            if(prefetchIndex == 1)
		            	execute(addr+block_size, "3 data");
		            break;
				case '1':
					outStr[4] = " "+"访问类型："+"写数据"+"   "+"地址："+addr+"\n";
					outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+k/(1<<wayIndex)
							     +"   命中情况：不命中";
					total_num ++;
					write_num ++;
				    write_miss ++;
				    total_miss ++;
				    write_miss_rate = (double)write_miss/write_num;
				    miss_rate = (double)total_miss/total_num;
				    
				    if(allocIndex == 0)  // 是否写分配
				    {
				    	if(writeIndex == 0)  // 是否写回
				    		dirty[k] = true;
				    	// 下面维持替换策略所需的参数
			            for(int j=0; j<1<<wayIndex; j++)
			            	if(cache[locate + j][0] != -1)
			            		cache[locate + j][1] ++;
			            cache[k][1] = 0;
			            // 下面操作代表将块读入
			            cache[k][0] = (int)(memory_num/cache_num);
				    }
				    break;
				case '2': 
					outStr[4] = " "+"访问类型："+"读指令"+"   "+"地址："+addr+"\n";
					outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+k/(1<<wayIndex)
							     +"   命中情况：不命中";
					total_num ++;
					read_inst_num ++;
					read_inst_miss ++;
					total_miss ++;
					read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
					miss_rate = (double)total_miss/total_num;
        	   	  
					for(int j=0; j<1<<wayIndex; j++)
						if(cache[locate + j][0] != -1)
							cache[locate + j][1] ++;
					cache[k][1] = 0;
					cache[k][0] = (int)(memory_num/cache_num);
					dirty[k] = false;
					if(prefetchIndex == 1)
						execute(addr+block_size, "3 inst");
					break;
				case '3': 
					outStr[4] = " "+"访问类型："+"读数据"+"   "+"地址："+addr+"\n";
					outStr[5] = "\t\t"+"块号："+(addr/block_size)+"   块内地址："+(addr%block_size)+"   索引："+k/(1<<wayIndex)
							     +"   命中情况：不命中";
					total_num ++;
		            total_miss ++;
		            miss_rate = (double)total_miss/total_num;
		            if(inst.charAt(0) == 'd')
		            {
		            	read_data_num ++;
		            	read_data_miss ++;
		            	read_data_miss_rate = (double)read_data_miss/read_data_num;
		            }
		            else if(inst.charAt(0) == 'i')
		            {
		            	read_inst_num ++;
		            	read_inst_miss ++;
		            	read_inst_miss_rate = (double)read_inst_miss/read_inst_num;
		            }	
		            dirty[k] = false;
		            cache[k][0] = (int)(memory_num/cache_num);
		            for(int j=0; j<1<<wayIndex; j++)
		            	if(cache[locate + j][0] != -1)
		            		cache[locate + j][1] ++;
		            cache[k][1] = 0;
		            break;
				default: System.out.println("error3!!");
			    }//switch
			}//if
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	
	public static void main(String[] args) {
		new CCacheSim();
	}
	

	public long hexStrToLong(String str)
	{
	    if (str == null) {
	        return 0;
	    }
	    if (str.length() == 0) {
	        return 0;
	    }
	    long num = 0;
	    str.toLowerCase();
	    //char[] strchar = new char[str.length()];
	    for(int i=0; i<str.length(); i++)
	    {
	    	switch(str.charAt(i)) {
	    	case '0': num <<= 4;  num += 0;  break;
	    	case '1': num <<= 4;  num += 1;  break;
	    	case '2': num <<= 4;  num += 2;  break;
	    	case '3': num <<= 4;  num += 3;  break;
	    	case '4': num <<= 4;  num += 4;  break;
	    	case '5': num <<= 4;  num += 5;  break;
	    	case '6': num <<= 4;  num += 6;  break;
	    	case '7': num <<= 4;  num += 7;  break;
	    	case '8': num <<= 4;  num += 8;  break;
	    	case '9': num <<= 4;  num += 9;  break;
	    	case 'a': num <<= 4;  num += 10;  break;
	    	case 'b': num <<= 4;  num += 11;  break;
	    	case 'c': num <<= 4;  num += 12;  break;
	    	case 'd': num <<= 4;  num += 13;  break;
	    	case 'e': num <<= 4;  num += 14;  break;
	    	case 'f': num <<= 4;  num += 15;  break;
	    	default : return num;
	    	}
	    }
	    return num;
	}

	
	/**
	 * 绘制 Cache 模拟器图形化界面
	 * 无需做修改
	 */
	public void draw() {
		//模拟器绘制面板
		setLayout(new BorderLayout(5,5));
		panelTop = new JPanel();
		panelLeft = new JPanel();
		panelRight = new JPanel();
		panelBottom = new JPanel();
		panelTop.setPreferredSize(new Dimension(800, 50));
		panelLeft.setPreferredSize(new Dimension(300, 450));
		panelRight.setPreferredSize(new Dimension(500, 450));
		panelBottom.setPreferredSize(new Dimension(800, 100));
		panelTop.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelLeft.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelRight.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelBottom.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		//*****************************顶部面板绘制*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************左侧面板绘制*****************************************//
		labelLeft = new JLabel("Cache 参数设置");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache 大小设置
		csLabel = new JLabel("总大小");
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache 块大小设置
		bsLabel = new JLabel("块大小");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//相连度设置
		wayLabel = new JLabel("相联度");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//替换策略设置
		replaceLabel = new JLabel("替换策略");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//欲取策略设置
		prefetchLabel = new JLabel("预取策略");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//写策略设置
		writeLabel = new JLabel("写策略");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//调块策略
		allocLabel = new JLabel("写不命中调块策略");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//选择指令流文件
		fileLabel = new JLabel("选择指令流文件");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setPreferredSize(new Dimension(210,30));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("浏览");
		fileBotton.setPreferredSize(new Dimension(70,30));
		fileBotton.addActionListener(this);
		
		panelLeft.add(labelLeft);
		panelLeft.add(csLabel);
		panelLeft.add(csBox);
		panelLeft.add(bsLabel);
		panelLeft.add(bsBox);
		panelLeft.add(wayLabel);
		panelLeft.add(wayBox);
		panelLeft.add(replaceLabel);
		panelLeft.add(replaceBox);
		panelLeft.add(prefetchLabel);
		panelLeft.add(prefetchBox);
		panelLeft.add(writeLabel);
		panelLeft.add(writeBox);
		panelLeft.add(allocLabel);
		panelLeft.add(allocBox);
		panelLeft.add(fileLabel);
		panelLeft.add(fileAddrBtn);
		panelLeft.add(fileBotton);
		
		//*****************************右侧面板绘制*****************************************//
		//模拟结果展示区域
		rightLabel = new JLabel("  模拟结果");
		rightLabel.setPreferredSize(new Dimension(500, 40));
		results = new JTextField[4];
		for (int i=0; i<4; i++) {
			results[i] = new JTextField("");
			results[i].setPreferredSize(new Dimension(500, 40));
		}
		
		stepLabel1 = new JLabel("");
		stepLabel1.setVisible(true);
		stepLabel1.setPreferredSize(new Dimension(500, 40));
		stepLabel2 = new JLabel("");
		stepLabel2.setVisible(true);
		stepLabel2.setPreferredSize(new Dimension(500, 40));
		
		panelRight.add(rightLabel);
		for (int i=0; i<4; i++) {
			panelRight.add(results[i]);
		}
		
		panelRight.add(stepLabel1);
		panelRight.add(stepLabel2);


		//*****************************底部面板绘制*****************************************//
		
		bottomLabel = new JLabel("执行控制");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("步进");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("执行到底");
		execAllBtn.setLocation(300, 30);
		execAllBtn.addActionListener(this);
		
		panelBottom.add(bottomLabel);
		panelBottom.add(execStepBtn);
		panelBottom.add(execAllBtn);

		add("North", panelTop);
		add("West", panelLeft);
		add("Center", panelRight);
		add("South", panelBottom);
		setSize(820, 620);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
