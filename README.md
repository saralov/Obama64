# Obama64
an encryption algorithm, like Base64.
用于对字符进行编码/解码，类似Base64，可将字符编码为限定码表范围内，编码为不易识别的ASCII字符序列，并且可根据该编码序列进行解码还原。
Obama64可用于如下等场景：
a).需要将字符编码为URLSafe的情况：比如需要将字符信息作为URL的一部分，或者HTTP报文头的一部分.
b).需要将字符进行弱加密的情况：cookie/URL中存放一些敏感信息，配置文件存放账号信息等.
