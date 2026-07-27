This app is in Chinese only — so if you don’t read Chinese, I’m afraid you’re out of luck.
(Not that the Chinese is anything to write home about either, tbh 😅)

If, by some miracle, anyone actually ends up using this, I might reboot it as an English version someday.

And yes, I know Java 5 is ancient history — but it’s what I’ve got, so we’re rolling with it.
Next up is the Chinese version of the README.
If you can't read it, you might want to have a translator handy — just saying.

------this is a line.------

如你所见，这是一个中文app，这是安卓的来着
但是我的开发环境导致我代码版本不到java的一般水准
所以这是一个介绍，我要开始介绍了
首先，你进入app时的页面应该是
<img width="1604" height="720" alt="Screenshot_20260727_103850_com mycompany galbianji" src="https://github.com/user-attachments/assets/1a985c5d-0402-4ce5-b106-ebe2136e3080" />
这样的界面，你会看到左侧有一个条，分为三分区
其为图片分区，音频分区，功能分区
长按对应分区的略缩条0.8秒就能展开了
图片和音频分区可以操作导入的图片和音频（你只需要按下即可）
等等……你说你在代码里看到了图片拖拽？不不不，那个功能因为触摸问题导致废弃，但代码没删罢了
功能分区则稍微复杂了一些
<img width="1604" height="720" alt="Screenshot_20260727_104743_com mycompany galbianji" src="https://github.com/user-attachments/assets/74cd4c51-5a68-4d6d-86e9-a40c8a496249" />
但他们通俗易懂！
点击就能用到对应功能！
接下来会介绍“项目”按钮（我讨厌这个）
你可以通过菜单和侧边栏-功能分区进入项目总览界面
<img width="1604" height="720" alt="Screenshot_20260727_105356_com mycompany galbianji" src="https://github.com/user-attachments/assets/ba1c911b-530b-4a93-a44e-f198dd1d8b95" />
为什么没用？因为不可抗力，导致绘画位置与实际可触摸位置差了20像素！你应该向上触摸！
功能方面也通俗易懂
但是这个东西的bug明显不少，最好不要用
那么就是设置部分了
（我已经不行了，编辑部分的教程自己去看源码吧）
三个存储类型
应用内部存储，你不可能看到的那部分
应用包存储，你可以在/sdcard/Android/data/com.mycompany.galbianji来找到他们
外部存储，你可以在/sdcard/galgalbianjiqigamebianjiqi中找到他们
右侧的条叫做缩放条，可以百分比缩放！
最大500%！
