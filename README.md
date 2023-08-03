# AndroidVideoExtractCombineDemo
Android Video Extract And Combine Demo

This is a demo for video extract and combime in Android.

When combining two videos into one, if the INFO (metadata) of the two videos is different, it will not play smoothly when reaching the duration of the second video, even if the lengths are compatible. If both videos are taken with the same device and have the same format, there should be no issues with the merging process. However, if the videos are captured with different devices, there might be problems due to varying formats that prevent seamless merging. In such cases, it is recommended to convert the video formats to the same standard before attempting to combine them.

在合成兩個影片為一個時，如果兩個影片的INFO不一樣，時長沒問題，但是到第二個影片的時長時不會播放
如果是同一隻手機拍的影片，因為格式一樣合成沒問題
不同手機的影片可能會有格式不同無法合併的問題，請先將影片格式轉檔到一樣的格式再合併
