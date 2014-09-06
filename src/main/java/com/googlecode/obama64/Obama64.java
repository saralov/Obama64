/** 
 *  @author zhongqiu
 *  @email 84281551@qq.com
 *  @version 1.3
 *  Date: 2011-10-13
 * */
package com.googlecode.obama64;

import java.util.Arrays;
import java.util.Random;

/**
 * 用于对字符进行编码/解码，功能类似Base64，可将字符编码为限定码表范围内，且不易识别的ASCII字符序列，
 * 并且可根据该编码序列进行解码还原。编解码速度与apache common codec提供的Base64相当，
 * 远快于JDK自带的sun的Base64实现.
 * 
 * Obama64可用于如下等场景：
 * 		a).需要将字符编码为URLSafe的情况：比如需要将字符信息作为URL的一部分，或者HTTP报文头的一部分.
 * 		b).需要将字符进行弱加密的情况：cookie/URL中存放一些敏感信息，配置文件存放账号信息等.
 *
 * 相较Base64进行编解码的一些特殊功能：
 * 		a).Base64编码后字符长度为原串的4/3，即增加1/3长度。而使用Obama64.encode()编码也是增加1/3长度，
 * 	而使用Obama64.encodeAscii()编码单字节(ASCII)字符增加长度为1/6.
 * 		b).提供编码扰乱功能，即相同字串可以编码成多个不同结果.
 * 		C).提供简单的自定义加/解密钩子.
 * 
 * 基本原理：
 * 		a).我们知道，ASCII编码的字符串，其每个字节的值范围是0~127，而其他多字节编码方案(GBK,UTF-8等)的字符串，字节值范围为0~255。
 * 	当范围为0~127时，1位(bit)恰好能表示其mod 64的情况：0表示[0~64),1表示[64,127]。当范围为0~255时，则需要2位(bit)
 *  来进行表示，00表示[0~64)，01表示[64~128)，10表示[128~192)，11表示[192~255).
 * 		a).与Base64将原串每6位(bit)组成一个新字节(byte)不同: encode时，Obama64采取的方式是使用在原串
 * 	每3个字节(如果是encodeAscii，则是6个字节)前插入1个字节，使用这个字节低6位(bit)的0|1值记录后续3个字节
 * 	(encodeAscii则是6个字节)mod 64的情况，因为ASCII字串，而原串各字节都转换为mod 64后对应码表的值.
 * 		b).decode时，则通过解码索引表找到编码字符mod 64前的值，然后根据其对应字节上记录的mod 64的情况，加上N*64还原到编码前的字节值.
 * 
 * @author zhong qiu
 */
public class Obama64 {

	/** 码表坐标掩码 */
	private static final int MASK_64 = 0x3F;
	
	/** 默认扰乱码 */
	private static final byte DEF_BLUFF_CODE = 0x43;
	
	/** 扰乱开关 */
	private boolean doBluff = false;
	
	/** 扰乱码 */
	private byte bluffCode = DEF_BLUFF_CODE;
	
	private final Random rand;
	
	/** 
	 * 编码基表。
	 * 原理，例如将字节x编码为y，则：
	 * y = encodeTable[x%64]
	 */
	private byte encodeTable[] = {
		// 默认序列
	    'P', 'e', 'r', 'Q', 'f', 'w', '7', 'g', 'i', 'p', 	/*  0- 9 */
	    '8', '9', 'B', 'd', 'O', 'v', '6', 'S', 'D', 'M', 	/* 10-19 */
	    'b', 's', 'R', 'C', 'N', 'c', 'm', '5', 'l', 'z', 	/* 20-29 */
	    'I', 'X', 'o', 'j', 'H', '2', 'x', 'W', '1', 'J', 	/* 30-39 */
	    'V', 'h', 'G', '0', 'Y', 'q', 'E', 'T', 'k', '3', 	/* 40-49 */
	    'a', 'L', 'y', 'n', 't', 'U', 'u', 'Z', '4', 'K', 	/* 50-59 */
	    'F', 'A', '_', '-'									/* 60-63 */  
	};
	
	/** 
	 * 解码参照表，主要用于快速索引，解码时，通过该索引表可以快速反查字节编码前的mod64后的值。
	 * 其下标值为编码后字符的ASCII值，而值为ASCII值在码表中对应下标。
	 * 例如将字节x编码为y: y = encodeTable[x%64]
	 * 则解码时：x = decodeTable[y]
	 */
	private int decodeTable[] = {
		// 参照编码表默认序列的值序
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  			/*  0- 9 */
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  			/*  10- 19 */
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  			/*  20- 29 */
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  			/*  30- 39 */
		-1, -1, -1, -1, -1, 63, -1, -1, 43, 38,  			/*  40- 49 */
		35, 49, 58, 27, 16, 6, 10, 11, -1, -1,  			/*  50- 59 */
		-1, -1, -1, -1, -1, 61, 12, 23, 18, 46,  			/*  60- 69 */
		60, 42, 34, 30, 39, 59, 51, 19, 24, 14,  			/*  70- 79 */
		0, 3, 22, 17, 47, 55, 40, 37, 31, 44,  				/*  80- 89 */
		57, -1, -1, -1, -1, 62, 0, 50, 20, 25,  			/*  90- 99 */
		13, 1, 4, 7, 41, 8, 33, 48, 28, 26,  				/*  100- 109 */
		53, 32, 9, 45, 2, 21, 54, 56, 15, 5,  				/*  110- 119 */
		36, 52, 29, -1, -1, -1, -1, -1, -1		 			/*  120- 128 */	
	};
	
	/** 
	 * 默认加密钩子 .
	 * 
	 * 可以通过实现自己的Encrypt，覆盖encode()、decode()方法，
	 * 并通过setEncrypt传递给Obama64来实现简单的自定义加解密过程
	 * 
	 * 实现Encrypt接口应遵循如下规则：
	 * 1.返回值范围在0-127之间, 且：
	 * 2.IF b = encode(a, secret); THEN a = decode(b, secret);
	 */
	private IEncrypt encrypt = new IEncrypt(){
		public byte encode(byte plaintext, byte secret) {
			return (byte)(plaintext ^ secret);
		}
		
		public byte decode(byte cryptograph, byte secret) {
			return (byte)(cryptograph ^ secret);
		}
	};
	
	public Obama64(){
		this.rand = new Random();
	}

	/**
	 * 是否绕乱编码结果
	 * @return 返回true表示对编码结果进行扰乱操作，false表示不进行扰乱
	 */
	public boolean isDoBluff() {
		return doBluff;
	}

	/**
	 * 设置扰乱开关
	 * @param bluff true打开，false关闭
	 */
	public void setDoBluff(boolean doBluff) {
		this.doBluff = doBluff;
	}

	/**
	 * 获取扰乱码
	 * @return bluff_code
	 */
	public byte getBluffCode() {
		return bluffCode;
	}

	/**
	 * 设置扰乱码
	 * @param bluffCode
	 */
	public void setBluffCode(byte bluffCode) {
		if(checkBluffCode(bluffCode))
			this.bluffCode = bluffCode;
		else
			throw new IllegalArgumentException("Invalid Bluff Code:" + bluffCode);
	}
	
	/**
	 * 检查扰乱码有效性，作为编码结果的一部分，限制其值在编码表encodeTable值范围内
	 * @param bluffCode 扰乱码
	 * @return 如果扰乱码不符合规定则返回false，否则返回true
	 */
	private boolean checkBluffCode(byte bluffCode){
		for(byte b : encodeTable){
			if(b == bluffCode)
				return true;
		}
		return false;
	}

	/**
	 * 设置自定义的简单加/接密过程
	 * @param encrypt
	 */
	public void setEncrypt(IEncrypt encrypt) {
		if(checkEncrypt(encrypt))
			this.encrypt = encrypt;
		else
			throw new IllegalArgumentException("Invalid Encrypt!");
	}
	
	/**
	 * 检查Encrypt的有效性
	 * @param encrypt 用户自定义Encrypt
	 * @return Encrypt合法则返回true，否则返回false
	 */
	private boolean checkEncrypt(IEncrypt encrypt){
		byte en = 0;
		for(byte b = 127; b>=0; b--){
			en = encrypt.encode(b, DEF_BLUFF_CODE);
			if(b != encrypt.decode(en, DEF_BLUFF_CODE))
				return false;
		}
		return true;
	}

	/**
	 * 重排序编码基表和对应的解码表
	 */
	public void init(){
		Random rand = new Random();
		// 重排序编码基表
		for (int i=encodeTable.length; i>1; i--)
            swap(encodeTable, i-1, rand.nextInt(i));
		
		// 根据重排序后的编码基表初始化解码表
		initDecodeTable();
	}
	
	/**
	 * 交换byte数组的指定两个元素
	 * @param arr
	 * @param i
	 * @param j
	 */
	private void swap(byte[] arr, int i, int j) {
        byte tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
	
	/**
	 * 根据编码基表，初始化解码参照表
	 */
	private void initDecodeTable(){
		Arrays.fill(decodeTable, -1);
		for(int i=0; i<encodeTable.length; i++){
			decodeTable[encodeTable[i]] = i;
		}
	}
	
	/**
	 * 根据解码参照表，初始化编码基表
	 */
	private void initEncodeTable(){
		for(int i=0; i<decodeTable.length; i++){
			if(decodeTable[i] < 0)
				continue;
			else
				encodeTable[decodeTable[i]] = (byte)i;
		}
	}
	
	/**
	 * 设置解码参照表，同时会根据设置的解码参照表初始化编码基表
	 * @param decode 解码参照表
	 */
	public void setDecodeTable(int[] decodeTable){
		this.decodeTable = decodeTable;
		initEncodeTable();
	}
	
	/**
	 * 设置编码基表，同时会根据设置的编码基表初始化解码参照表
	 * @param encode 编码基表
	 */
	public void setEncodeTable(byte[] encodeTable){
		this.encodeTable = encodeTable;
		initDecodeTable();
	}
	
	private byte bluffCode(){
		return encodeTable[rand.nextInt(64)];
	}
	
	/**
	 * 基本的编码方法
	 * @param content 待编码的字节序列
	 * @return 编码后的字节序列
	 */
	public byte[] encode(byte[] content){
		if(content==null || content.length==0)
			return content;
		
		int len = content.length;
		byte[] cArray = new byte[len + (int)Math.ceil(len/3.0d) + 1];
		if(doBluff){
			cArray[0] = bluffCode();
		}else{
			cArray[0] = bluffCode;
		}
		byte c = 0;
		int n = 0;
		int mark = 0;
		int pos = 0;
		int segs = 0;
		for(int i=0; i<len; i+=3){
			mark = 1+i+segs;
			for(int k=0; (k<3) && (pos<len); k++){
				c = content[pos];
				if(c<0){
					c = (byte)~c;
					cArray[mark] |= (2<<(k<<1));
				}
				n = encrypt.encode(c, cArray[0]);
				n ^= k;
				cArray[mark] |= ((n>>>6)<<(k<<1)); 
				cArray[mark+k+1] = (byte)encodeTable[n & MASK_64];
				pos++;
			}
			segs++;
			cArray[mark] = (byte)encodeTable[cArray[mark]];
		}
		
		return cArray;
	}
	
	/**
	 * 基本的解码方法，用于解码encode()生成的编码
	 * @param content encode()生成的编码字节序列
	 * @return 解码后的字节序列
	 */
	public byte[] decode(byte[] content){
		if(content==null || content.length==0)
			return content;
		
		int len = content.length;
		byte[] cArray = new byte[len - 1 - (int)Math.ceil((len-1)/4.0)];
		byte secret = content[0];
		byte c = 0;
		int mark = 0;
		int index = 0;
		int tmp = 0;
		for(int i=1; i<len; i+=4){
			mark = decodeTable[content[i]];
			for(int k=0, pos=i + k + 1; k<3 && pos<len; k++, pos++){
				c = content[pos];
				tmp = mark>>>(k<<1); 
				cArray[index] = (byte)encrypt.decode((byte)((decodeTable[c] + ((tmp&1)<<6))^ k), secret);
				if((tmp & 2) > 0)
					cArray[index] = (byte)~cArray[index];
				index++;
			}
		}
		
		return cArray;
	}
	
	
	/** 
	 * 编码纯ASCII字节序列，使用此方法产生的编码相对encode()短。
	 * <ul>
	 * <li>encode()产生的编码长度为 len + len/3 + 1，len代表待编码字节序列长度
	 * <li>encodeAscii()产生的编码长度为 len + len/6 + 1, len代表待编码字节序列长度
	 * </ul>
	 * @param content 待编码的ASCII字节序列
	 * @return 编码后的字节序列
	 * 
	 * 注意：使用该方法编码非ASCII字符将抛出ArrayIndexOutOfBoundsException。
	 * 如果不清楚待编码的字节序列是否是纯ASCII字符，那么应该选用encode()和decode()方法进行编/解码。
	 */
	public byte[] encodeAscii(byte[] content){
		if(content==null || content.length==0)
			return null;
		
		int len = content.length;
		byte[] cArray = new byte[len + (int)Math.ceil(len/6.0d) + 1];
		if(doBluff){
			cArray[0] = bluffCode();
		}else{
			cArray[0] = bluffCode;
		}
		byte c = 0;
		int n = 0;
		int mark = 0;
		int pos = 0;
		int segs = 0;
		for(int i=0; i<len; i+=6){
			mark = 1+i+segs;
			for(int k=0; (k<6) && (pos<len); k++){
				c = content[pos];
				n = encrypt.encode(c, cArray[0]);
				n ^= k;
				cArray[mark] |= ((n>>>6)<<k); 
				
				cArray[mark+k+1] = (byte)encodeTable[n & MASK_64];
				pos++;
			}
			segs++;
			cArray[mark] = (byte)encodeTable[cArray[mark]];
		}
		
		return cArray;
	}
	
	/**
	 * 解码由encodeAscii生成的字节序列
	 * 
	 * @param content 待解码的字节序列
	 * @return 解码后的字节序列
	 * 
	 * 
	 */
	public byte[] decodeAscii(byte[] content){
		if(content==null || content.length==0)
			return null;
		
		int len = content.length;
		byte[] cArray = new byte[len - 1 - (int)Math.ceil((len-1)/7.0)];
		byte secret = content[0];
		byte c = 0;
		int mark = 0;
		int index = 0;
		for(int i=1; i<len; i+=7){
			mark = decodeTable[content[i]];
			for(int k=0, pos=i + k + 1; k<6 && pos<len; k++, pos++){
				c = content[pos];
				cArray[index] = (byte)encrypt.decode((byte)((decodeTable[c] + (((mark>>>k)&1)<<6))^ k), secret);
				index++;
			}
		}
		
		return cArray;
	}
	
	/**
	 * 打印编码基表
	 */
	public void printBaseEncode(){
		int loop =0;
		for(byte n : encodeTable){
			System.out.print("'"+(char)n+"', ");
			loop++;
			if(loop%10==0)
				System.out.println("\t/* " + (loop-10) + " - " + (loop-1) + " */" );
		}
		System.out.println();
	}
	
	/**
	 * 打印解码参照表
	 */
	public void printBaseDecode(){
		int loop =0;
		for(int n : decodeTable){
			System.out.print((n<10&&n>-1?" ":"") + n + ", ");
			loop++;
			if(loop%10==0)
				System.out.println("\t/* " + (loop-10) + " - " + (loop-1) + " */");
		}
		System.out.println();
	}
	
	/**
	 * 加密钩子接口
	 */
	public static interface IEncrypt {
		byte encode(byte plaintext, byte secret);
		byte decode(byte cryptograph, byte secret);
	}
	

}
