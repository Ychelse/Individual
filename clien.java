import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends Thread implements ActionListener{
	//是否停止
	public static int STOP=0;
	
	//用户列表
	public static Map<String,SocketAddress> userMap=new HashMap();
	
	private DatagramSocket client;
	
	private JFrame frame;
	//聊天信息
	private JTextArea info;
	//在线用户
	private JTextArea onlineUser;
	private JTextField msgText;
	
	private JButton sendButton;
	
	public Client(DatagramSocket client)throws Exception
	{
//格式
		this.client=client;
		this.frame=new JFrame("P2P chat");
		frame.setSize(800, 400);
		
		sendButton=new JButton("send");
		JScrollBar scroll=new JScrollBar();
		this.info=new JTextArea(10,30);
		info.setLineWrap(true);
		info.setWrapStyleWord(true);
		info.setEditable(false);
		scroll.add(info);
		
		onlineUser=new JTextArea(10,30);
		onlineUser.setLineWrap(true);
		onlineUser.setWrapStyleWord(true);
		onlineUser.setEditable(false);
		
		
		JPanel infopanel=new JPanel();
		infopanel.add(info,BorderLayout.WEST);
		JPanel infopanel1=new JPanel();
		JLabel label=new JLabel("online user");
		infopanel1.add(label, BorderLayout.NORTH);
		infopanel1.add(onlineUser, BorderLayout.SOUTH);
		infopanel.add(infopanel1,BorderLayout.EAST);

		JPanel panel=new JPanel();
		
		msgText=new JTextField(30);
	
		panel.add(msgText);
		panel.add(sendButton);
		frame.add(infopanel,BorderLayout.NORTH);
		frame.add(panel,BorderLayout.SOUTH);
		frame.setVisible(true);
		
		sendButton.addActionListener(this);
		
		frame.addWindowListener(new   WindowAdapter(){ 
            public   void   windowClosing(WindowEvent   e){ 
                System.exit(0);
            } 
         }); 
		

	}
	
	 //心跳
	private void sendSkip()
	{
		new Thread(){
			public void run()
			{
				try
				{
					String msg="skip";
					while(true)
					{
						if(STOP==1)
							break;
						if(userMap.size()>0)
						{
							 for (Entry<String, SocketAddress> entry : userMap.entrySet()) {
								 DatagramPacket data=new DatagramPacket(msg.getBytes(),msg.getBytes().length,entry.getValue());
								client.send(data);
							}
						}
						//10s一次
						Thread.sleep(10*1000);
					}
				}catch(Exception e){}
				
			}
		}.start();
	}
	
	//接收数据
	public void run()
	{
		try
		{
			
			String msg;
			DatagramPacket data;
			
			//执行心跳
			sendSkip();
			
			while(true)
			{
				if(STOP==1)
					break;
				byte[] buf=new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				client.receive(packet);
				msg=new String(packet.getData(),0,packet.getLength());
				if(msg.length()>0)
				{
					if(msg.indexOf("server:")>-1)
					{
						//服务器数据
						String userdata=msg.substring(msg.indexOf(":")+1,msg.length());
						String[] user=userdata.split(",");
						for(String u:user)
						{
							if(u!=null&&u.length()>0)
							{
								String[] udata=u.split("#");
								String ip=udata[1].split(":")[0];
								int port=Integer.parseInt(udata[1].split(":")[1]);
								
								ip=ip.substring(1,ip.length());
								
								SocketAddress adds=new InetSocketAddress(ip,port);
								userMap.put(udata[0], adds);
								//发送空白报文
								data=new DatagramPacket(new byte[0],0,adds);
								client.send(data);
								
							}
							
						}
						//更新在线用户列表
						this.onlineUser.setText("");
						for (Map.Entry<String, SocketAddress> entry : userMap.entrySet()) {
							this.onlineUser.append("用户"+entry.getKey()+"("+entry.getValue()+")\n");
						}

					}
					else if(msg.indexOf("skip")>-1);
					else
					{
						//消息
						this.info.append(packet.getAddress().toString()+packet.getPort()+" 说："+msg);
						this.info.append("\n");
					}
				}
			}
		}
		catch(Exception e){}
	}
	
	public static void main(String args[])throws Exception
	{
		
		String serverIP="122.225.99.40";///122.225.99.40
		int port=6636;
		
		//构造一个目标地址
		SocketAddress target = new InetSocketAddress(serverIP, port); 
		
		DatagramSocket client = new DatagramSocket();
		String msg="report";
		byte[] buf=msg.getBytes();
		//向服务器发送数据
		DatagramPacket packet=new DatagramPacket(buf,buf.length,target);
		client.send(packet);
		new Client(client).start();
		
	}

	
	//按钮
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.sendButton)
		{
			try{
				String msg=this.msgText.getText();
				if(msg.length()>0)
				{
					this.info.append("Me:"+msg);
					this.info.append("\n");
					for (Map.Entry<String, SocketAddress> entry : userMap.entrySet()) {
						DatagramPacket data=new DatagramPacket(msg.getBytes(),msg.getBytes().length,entry.getValue());
						client.send(data);
					}
					
					this.msgText.setText("");
				}
			}
			catch(Exception ee){}
		}
		
	}
}
