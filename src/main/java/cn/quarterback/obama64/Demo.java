package cn.quarterback.obama64;

import cn.quarterback.obama64.Obama64.IEncrypt;

/**
 * 使用示例
 * @author zhong qiu
 */
public class Demo {

	public static void main(String[] args) {
		//创建编/解码器
		Obama64 obama64 = new Obama64();
		
		//测试字串
		String content = "用户名:9527;密码:7259";
		
		/** Test 1: 基本的编解码***************************************************/
		//编码
		byte[] encodeBytes = obama64.encode(content.getBytes());
		String encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		byte[] decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		/** Test 2: 打开bluff开关***************************************************/
		obama64.setDoBluff(true);
		//编码
		encodeBytes = obama64.encode(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		/** Test 3: 编码纯ASCII字符*************************************************/
		content = "http://cdn2.down.apk.gfan.com/asdf/Pfiles/2011/12/6/201217_9c539a53-1623-4ad3-8312-5782bb072e3e.apk";
		//编码
		encodeBytes = obama64.encode(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		/* 使用encodeAscii方法可以得到更短的编码序列 */
		//编码
		encodeBytes = obama64.encodeAscii(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decodeAscii(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		/** Test 4: 自定义简单加解密过程 **********************************************/
		IEncrypt myEncrypt1 = new IEncrypt() {
			public byte encode(byte plaintext, byte secret) {
				 return (byte)(plaintext > 63 ? plaintext - 64 : plaintext + 64);
			}
			
			public byte decode(byte cryptograph, byte secret) {
				return (byte)(cryptograph > 63 ? cryptograph - 64 : cryptograph + 64);
			}
		};
		obama64.setEncrypt(myEncrypt1);
		//编码
		encodeBytes = obama64.encode(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		IEncrypt myEncrypt2 = new IEncrypt() {
			public byte encode(byte plaintext, byte secret) {
				return (byte)(plaintext ^ 7);
			}
			
			public byte decode(byte cryptograph, byte secret) {
				return (byte)(cryptograph ^ 7);
			}
		};
		obama64.setEncrypt(myEncrypt2);
		//编码
		encodeBytes = obama64.encode(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
		
		/** Test 3: 通过重排序编码表，也可以产生区别化的编码*****************************/		
		obama64.printBaseEncode();
		System.out.println("-------------------------");
		obama64.init();
		obama64.printBaseEncode();
		//编码
		encodeBytes = obama64.encode(content.getBytes());
		encodeString = new String(encodeBytes);
		System.out.println("编码："+encodeString);
		//解码
		decodeBytes = obama64.decode(encodeString.getBytes());
		System.out.println("解码："+new String(decodeBytes));
		System.out.println();
	}

}
