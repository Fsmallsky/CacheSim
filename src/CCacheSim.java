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


    //��������
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "ֱ��ӳ��", "2·", "4·", "8·", "16·", "32·" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "��Ԥȡ", "������Ԥȡ" };
	private String write[] = { "д�ط�", "дֱ�﷨" };
	private String alloc[] = { "��д����", "����д����" };
	//private String typename[] = { "������", "д����", "��ָ��" };
	//private String hitname[] = {"������", "����" };
	
	//�Ҳ�����ʾ
	private String rightLable1[]={"�����ܴ�����","��ָ�������","�����ݴ�����","д���ݴ�����"};
	private String rightLable2[]={"ȱʧ�ܴ�����","��ָ��ȱʧ����","������ȱʧ����","д����ȱʧ����"};
	private String rightLable3[]={"��ȱʧ�ʣ�","��ָ��ȱʧ�ʣ�","������ȱʧ�ʣ�","д��ȱʧ�ʣ�"};
	private String outStr[];
	//���ļ�
	private File file;
	
	//�ֱ��ʾ��༸����������ѡ��ĵڼ�������� 0 ��ʼ
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	//          �ܴ�С *4          ���С*2            ������                      LRU/FIFO/RAND    ��/Ԥȡ                      д��/дֱ��             ��/���� д���� 
	//������������
	private FileReader file_reader;   //�ļ��ֽ���
	private BufferedReader in;  //�ļ��е�һ��
	private int cache[][];   //cache�����ݽṹ��
	private boolean dirty[];  // cache����Ƿ�Ϊ��ı�־
	private int total_num, total_miss, read_data_num, read_data_miss, read_inst_num, 
	            read_inst_miss, write_num, write_miss;  //���ַ��ʴ�����ȱʧ��
	private double miss_rate, read_data_miss_rate, read_inst_miss_rate, write_miss_rate;   //ȱʧ����
	private long cache_num, block_size, cache_size;  //cache�еĿ���Ŀ��ÿ����ֽ�����cache���ܴ�С
	private int row;  //cache�еĿ���Ŀ
	
	
	
	/*
	 * ���캯��������ģ�������
	 */
	public CCacheSim(){
		super("Cache Simulator");
		draw();
	}
	
	
	//��Ӧ�¼������������¼���
	//   1. ִ�е����¼�
	//   2. ����ִ���¼�
	//   3. �ļ�ѡ���¼�
	public void actionPerformed(ActionEvent e){
				
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep();
		}
		if (e.getSource() == fileBotton){
			fileChoose = new JFileChooser("F:\\������\\��ϵ�ṹ\\experiments\\lab2\\MyCache\\MyCacheģ����\\��ַ��");
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
	 * ��ʼ�� Cache ģ����
	 * cache�п�Ϊ0,��Ϊ-1������Ϊ1
	 */
	public void initCache() {
		outStr = new String[6];
	    row = (2048<<(2*csIndex))/(16<<bsIndex);
		cache = new int[row][2];   //value ��    ��־
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
	 * ��ָ������������ļ��ж���
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
	 * ģ�ⵥ��ִ��
	 */
	public void simExecStep() {
		String inst;
		String[] mRateStr = new String[4];
		DecimalFormat df = new DecimalFormat("0.00");
		try {
			inst = in.readLine();
			if(inst != null && inst.length() != 0)
			{
				long addr = hexStrToLong(inst.substring(2));  //��ʮ�������ַ���ת��ΪLong������
				execute(addr, inst);
				//��������������ַ�����ͼ�ν������
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
	 * ģ��ִ�е���
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
							outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   �������������";
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
						case '1':  // ��������д���ݲ���
							// �������ַ������ڲ����ǽ�������
							outStr[4] = " "+"�������ͣ�"+"д����"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   �������������";
							total_num ++;
							write_num ++;
							write_miss_rate = (double)write_miss/write_num;
							miss_rate = (double)total_miss/total_num;
							if(replaceIndex == 0)
							{ // ������Ϊ�滻���ԵĲο�ֵ
								for(int j=0; j<1<<wayIndex; j++)
									if(cache[locate + j][0] != -1)
										cache[locate + j][1]++;
								cache[locate + i][1] = 0;
							}
							if(writeIndex == 0)
								dirty[locate + i] = true;
							break;
						case '2': 
							outStr[4] = " "+"�������ͣ�"+"��ָ��"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   �������������";
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
							outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   �������������";
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
					{//miss �� cache���ÿ�λ��ֱ�Ӷ���
						succ = true;
						switch(inst.charAt(0)){
						case '0': 
							outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr;
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   ���������������";
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
							outStr[4] = " "+"�������ͣ�"+"д����"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   ���������������";
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
							outStr[4] = " "+"�������ͣ�"+"��ָ��"+"   "+"��ַ��"+addr+"\n";
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   ���������������";
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
		          		    cache[locate + i][0] = (int)(memory_num/cache_num); // ������� 
		          		    dirty[locate + i] = false;
		          		    if(prefetchIndex == 1)  // �Ƿ�Ԥȡ
		          		    	execute(addr+block_size, "3 inst");
		          		    break;
						case '3':
							outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr;
							outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+(locate/(1<<wayIndex))
									     +"   ���������������";
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
				            cache[locate + i][0] = (int)(memory_num/cache_num);  // ���˿����cache
				             // ����ά���滻ʱ��Ҫ�Ĳ���
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
			{//miss ����Ҫ�滻��
				int max = -1;
				int k = locate;   //��Ҫ�滻�Ŀ��λ��
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
			    	k =  locate+2;      //����һ�������
			    }
			    switch(inst.charAt(0)){
				case '0':
					outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr+"\n";
					outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+k/(1<<wayIndex)
							     +"   ���������������";
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
					outStr[4] = " "+"�������ͣ�"+"д����"+"   "+"��ַ��"+addr+"\n";
					outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+k/(1<<wayIndex)
							     +"   ���������������";
					total_num ++;
					write_num ++;
				    write_miss ++;
				    total_miss ++;
				    write_miss_rate = (double)write_miss/write_num;
				    miss_rate = (double)total_miss/total_num;
				    
				    if(allocIndex == 0)  // �Ƿ�д����
				    {
				    	if(writeIndex == 0)  // �Ƿ�д��
				    		dirty[k] = true;
				    	// ����ά���滻��������Ĳ���
			            for(int j=0; j<1<<wayIndex; j++)
			            	if(cache[locate + j][0] != -1)
			            		cache[locate + j][1] ++;
			            cache[k][1] = 0;
			            // ����������������
			            cache[k][0] = (int)(memory_num/cache_num);
				    }
				    break;
				case '2': 
					outStr[4] = " "+"�������ͣ�"+"��ָ��"+"   "+"��ַ��"+addr+"\n";
					outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+k/(1<<wayIndex)
							     +"   ���������������";
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
					outStr[4] = " "+"�������ͣ�"+"������"+"   "+"��ַ��"+addr+"\n";
					outStr[5] = "\t\t"+"��ţ�"+(addr/block_size)+"   ���ڵ�ַ��"+(addr%block_size)+"   ������"+k/(1<<wayIndex)
							     +"   ���������������";
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
	 * ���� Cache ģ����ͼ�λ�����
	 * �������޸�
	 */
	public void draw() {
		//ģ�����������
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

		//*****************************����������*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************���������*****************************************//
		labelLeft = new JLabel("Cache ��������");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache ��С����
		csLabel = new JLabel("�ܴ�С");
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache ���С����
		bsLabel = new JLabel("���С");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//����������
		wayLabel = new JLabel("������");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//�滻��������
		replaceLabel = new JLabel("�滻����");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//��ȡ��������
		prefetchLabel = new JLabel("Ԥȡ����");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//д��������
		writeLabel = new JLabel("д����");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//�������
		allocLabel = new JLabel("д�����е������");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//ѡ��ָ�����ļ�
		fileLabel = new JLabel("ѡ��ָ�����ļ�");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setPreferredSize(new Dimension(210,30));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("���");
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
		
		//*****************************�Ҳ�������*****************************************//
		//ģ����չʾ����
		rightLabel = new JLabel("  ģ����");
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


		//*****************************�ײ�������*****************************************//
		
		bottomLabel = new JLabel("ִ�п���");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("����");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("ִ�е���");
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
