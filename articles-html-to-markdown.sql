-- 旧 HTML 文章正文迁移为 Markdown
-- 由数据库导出文件离线生成；仅更新检测到 HTML 的记录。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `articles_content_backup_20260722095827` AS
SELECT id, content, updatedAt FROM articles WHERE id IN (18, 19, 22, 27, 29, 30, 32, 33, 34, 35, 54, 56, 57, 58, 60, 62);

START TRANSACTION;
-- 18: 力扣算法题目
UPDATE articles SET content = '#    第一题：两数之和

### **题目内容**：

给定一个整数数组 **nums** 和一个整数目标值 **target**，请你在该数组中找出 **和为目标值** **target** 的那 **两个** 整数，并返回它们的数组下标。你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。你可以按任意顺序返回答案。例如：

输入：nums = \\[2,7,11,15\\], target = 9

输出：\\[0,1\\]

解释：因为 nums\\[0\\] + nums\\[1\\] == 9 ，返回 \\[0, 1\\] 。

### **题解：**

首先创建preN数组，存入先前遍历的数字，，preN\\[数字\\]=坐标，从1到nums.length,遍历，如果preN\\[target-nums\\[i\\]\\]的值存在，说明在先前遍历时，数组中存在该数，，于是可以返回preN\\[target-nums\\[i\\]\\]，和当前i值，作为答案输出。

```
/**
 * @param {number[]} nums
 * @param {number} target
 * @return {number[]}
 */
var twoSum = function(nums, target) {
    const preNums={}
    for(let i=0;i<nums.length;i++){
      const curN=nums[i];
      const tarN=target-curN;
      const tarNIndex=preNums[tarN];
      if(tarNIndex!=undefined)
      {
        return [tarNIndex,i];
      }
      else
      {
        preNums[curN]=i;
      }
    }
};
```

#   第二题：字母异位词分组

### **题目内容**：

给你一个字符串数组，请你将 字母异位词 组合在一起。可以按任意顺序返回结果列表。

**示例 1:**

**输入:** strs = \\["eat", "tea", "tan", "ate", "nat", "bat"\\]

**输出:** \\[\\["bat"\\],\\["nat","tan"\\],\\["ate","eat","tea"\\]\\]

**解释：**

-   在 strs 中没有字符串可以通过重新排列来形成 "bat"。
-   字符串 "nat" 和 "tan" 是字母异位词，因为它们可以重新排列以形成彼此。
-   字符串 "ate" ，"eat" 和 "tea" 是字母异位词，因为它们可以重新排列以形成彼此。

### **题解：**

首先设置一个map键值对，map获取值的方法是map.get(key),循环遍历strs数组，首先先将str数组化：Array.from(str),进行sort排序，再字符串化，因为数组在js中是引用类型，js中每一个数组都是独一无二的，哪怕他们的值相同，所以要将他们转化为字符串才行。

```
/**
 * @param {string[]} strs
 * @return {string[][]}
 */
var groupAnagrams = function(strs) {
    //map,存储排好序的，相应的字符串数组
    const map=new Map();
    for(let str of strs)
    {

         let ostr=Array.from(str).sort();
         const key=ostr.toString();
         let list=map.get(key)?map.get(key):new Array();
         list.push(str);
         map.set(key,list);
    }
    return Array.from(map.values());
};
```

#   第三题：最长连续子序列

### **题目内容**：

给定一个未排序的整数数组 nums ，找出数字连续的最长序列（不要求序列元素在原数组中连续）的长度。

请你设计并实现时间复杂度为 O(n) 的算法解决此问题。

**示例 1：**

输入：nums = \\[100,4,200,1,3,2\\]

输出：4

解释：最长数字连续序列是 \\[1, 2, 3, 4\\]。它的长度为 4。

### **题解：**

首先创建一个set集合去重，循环遍历集合元素，当前元素为x，如果存在x-1，就continue，而后循环y=x+1，如果y存在，y++，不存在跳出循环，y-x就是ans，取最大ans即可。

```
/**
 * @param {number[]} nums
 * @return {number}
 */
var longestConsecutive = function(nums) {
    //思路，定义一个set数组，
    //循环遍历每个元素，如果存在x-1就continue，不存在循环y=x+1;看y在不在，y-x就是ans
    let ans=0;
    let snums=new Set(nums);
    for(const n of snums)
    {
        if(snums.has(n-1))
         continue;
        let y=n+1;
        while(snums.has(y))
         y++;
        ans=Math.max(ans,y-n);
    }
    return ans;
};
```

# 第四题：移动零

### **题目内容**：

给定一个数组 nums，编写一个函数将所有 0 移动到数组的末尾，同时保持非零元素的相对顺序。

**请注意** ，必须在不复制数组的情况下原地对数组进行操作。

**示例 1：**

输入：\\[0,1,0,3,12\\]

输出：\\[1,3,12,0,0\\]

### **题解：**

循环遍历数组元素，定义变量zeroNums表示0的数量，如果元素不为0，将当前元素向前移zeroNums位，当前位置设置为0。

```
/**
 * @param {number[]} nums
 * @return {void} Do not return anything, modify nums in-place instead.
 */
var moveZeroes = function(nums) {
    let zeroNums=0;
    for(let i=0;i<nums.length;i++)
    {
        if(!nums[i]) zeroNums++;
        else{
            [nums[i],nums[i-zeroNums]]=[0,nums[i]];
        }
    }
};
```

# 第五题：盛最多水的容器

### **题目内容**：

给定一个长度为 n 的整数数组 height 。有 n 条垂线，第 i 条线的两个端点是 (i, 0) 和 (i, height\\[i\\]) 。

找出其中的两条线，使得它们与 x 轴共同构成的容器可以容纳最多的水。

返回容器可以储存的最大水量。

**说明：**你不能倾斜容器。

输入：\\[1,8,6,2,5,4,8,3,7\\]

输出：49

解释：图中垂直线代表输入数组 \\[1,8,6,2,5,4,8,3,7\\]。在此情况下，容器能够容纳水（表示为蓝色部分）的最大值为 49。

### **题解：**

双指针算法，容量s=(j-i)\\*min(a\\[j\\],a\\[i\\]),如果a\\[i\\]>a\\[j\\],则j--，反之则i++，求取最大值。

```javascript
/**
 * @param {number[]} height
 * @return {number}
 */
var maxArea = function(height) {
    //  s=(j-i)*min(a[j],a[i])
    //双指针，a[i],a[j]谁小，哪个指针移动
    let ans=0;
    for(let i=0,j=height.length-1;i<j; )
    {
        let width=j-i;
        if(height[i]>height[j])
        {
            ans=Math.max(ans,width*height[j]);
            j--;
        }
        else{
            ans=Math.max(ans,width*height[i]);
            i++;
        }
    }
    return ans;
};
```' WHERE id = 18;

-- 19: 力扣算法笔记
UPDATE articles SET content = '# 第一题：三数之和

### **题目内容**：

> 给你一个整数数组 nums ，判断是否存在三元组 \\[nums\\[i\\], nums\\[j\\], nums\\[k\\]\\] 满足 i != j、i != k 且 j != k ，同时还满足 nums\\[i\\] + nums\\[j\\] + nums\\[k\\] == 0 。请你返回所有和为 0 且不重复的三元组。

注意：答案中不可以包含重复的三元组。

**示例 1：**

输入：nums = \\[-1,0,1,2,-1,-4\\]

输出：\\[\\[-1,-1,2\\],\\[-1,0,1\\]\\]

解释：

nums\\[0\\] + nums\\[1\\] + nums\\[2\\] = (-1) + 0 + 1 = 0 。

nums\\[1\\] + nums\\[2\\] + nums\\[4\\] = 0 + 1 + (-1) = 0 。

nums\\[0\\] + nums\\[3\\] + nums\\[4\\] = (-1) + 2 + (-1) = 0 。

不同的三元组是 \\[-1,0,1\\] 和 \\[-1,-1,2\\] 。

注意，输出的顺序和三元组的顺序并不重要。

### **题解：**

使用双指针算法，先排序，第一层从0到length-2循环遍历，如果nums\\[k\\]>0,说明之后sum都大于0，break。要考虑nums\\[k\\]的去重。第二层使用while循环，i=k+1，j=length-1，sum等于0，push结果，而后考虑nums\\[i\\],nums\\[j\\]的去重，使用while++--进行去重。

```
/**
 * @param {number[]} nums
 * @return {number[][]}
 */
var threeSum = function (nums) {
  let onums = nums.sort((a, b) => a - b)
  let ans = []
  for (let k = 0; k < onums.length - 1; k++) {
    if (onums[k] > 0) break
    if (onums[k] == onums[k - 1] && k > 0) continue
    let i = k + 1

    let j = onums.length - 1
    while (i < j) {
      let sum = onums[k] + onums[i] + onums[j]
      if (sum == 0) {
        ans.push([onums[k], onums[i], onums[j]])
        while (i < j &&  onums[i] == onums[i + 1]) i++
        while (i < j &&  onums[j] == onums[j - 1]) j--
        i++
        j--
      }
      else if (sum < 0) i++
      else if (sum > 0) j--
    }
  }
  return ans
}
```

# 第二题：接雨水

### **题目内容**：

给定 n 个非负整数表示每个宽度为 1 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。

**示例 1：**

输入：height = \\[0,1,0,2,1,0,1,3,2,1,2,1\\]

输出：6

解释：上面是由数组 \\[0,1,0,2,1,0,1,3,2,1,2,1\\] 表示的高度图，在这种情况下，可以接 6 个单位的雨水（蓝色部分表示雨水）。

### **题解：**

使用双指针算法，创建leftmax，rightmax，指针移动逻辑为那一边小就移动那一边，如果左边小，则有leftMax<rightMax，就++left，ans+=leftMax-height\\[left\\],如果相等的，则移动right。

```
/**
 * @param {number[]} height
 * @return {number}
 */
var trap = function(height) {
   let ans=0;
   let right=height.length-1;
   let left=0;
   let leftmax=0;
   let rightmax=0;
   while(left<right)
   {
        leftmax=Math.max(height[left],leftmax);
        rightmax=Math.max(height[right],rightmax);
        if(height[left]<=height[right])
        {
            ans+=leftmax-height[left];
            left++;
        }
        else
        {
            ans+=rightmax-height[right];
            right--;
        }
   }
   return ans;
};
```

# 第三题：无重复字符的最长子串

### **题目内容**：

给定一个字符串 s ，请你找出其中不含有重复字符的 最长 子串 的长度。

**示例 1:**

输入: s = "abcabcbb"

输出: 3

解释: 因为无重复字符的最长子串是"abc"，所以其长度为 3。注意 "bca" 和 "cab" 也是正确答案。

### **题解：**

使用滑动窗口算法，窗口使用set集合，定义左指针left：负责删掉元素，右指针right，负责添加元素，左右指针区间构成一个滑动窗口，当右指针走到字符串末尾，循环结束。如果无重复元素，移动右指针，有则移动左指针。

```
/**
 * @param {string} s
 * @return {number}
 */
var lengthOfLongestSubstring = function (s) {
  let windows = new Set()
  let ans = 0
  let right = 0
  let left = 0
  while (right < s.length) {
    if (!windows.has(s[right])) {
      windows.add(s[right])
      right++
      ans = Math.max(ans, windows.size)
    } else {
      windows.delete(s[left])
      left++
    }
  }
  return ans
}

```

# 第四题：找到字符串中所有字母异位词

### **题目内容**：

给定两个字符串 s 和 p，找到 s 中所有 p 的 异位词 的子串，返回这些子串的起始索引。不考虑答案输出的顺序。

**示例 1:**

输入: s = "cbaebabacd", p = "abc"

输出: \\[0,6\\]

解释:

起始索引等于 0 的子串是 "cba", 它是 "abc" 的异位词。

起始索引等于 6 的子串是 "bac", 它是 "abc" 的异位词。

### **题解：**

使用滑动窗口算法，定义两个26长度数组记录字符数量，滑动时更新数组，只看这两个数组是否相同，相同就是异位词。

优化版，只用定义字符数量即可，比较差异字符是否为0。滑动时，更新字符数量，和differ大小，如果differ=0，就是异位词。

```
/**
 * @param {string} s
 * @param {string} p
 * @return {number[]}
 */
var findAnagrams = function(s, p) {
    const sLen = s.length, pLen = p.length;

    if (sLen < pLen) {
        return [];
    }

    const ans = [];
    const sCount = new Array(26).fill(0);
    const pCount = new Array(26).fill(0);
    for (let i = 0; i < pLen; ++i) {
        ++sCount[s[i].charCodeAt() - ''a''.charCodeAt()];
        ++pCount[p[i].charCodeAt() - ''a''.charCodeAt()];
    }

    if (sCount.toString() === pCount.toString()) {
        ans.push(0);
    }

    for (let i = 0; i < sLen - pLen; ++i) {
        --sCount[s[i].charCodeAt() - ''a''.charCodeAt()];
        ++sCount[s[i + pLen].charCodeAt() - ''a''.charCodeAt()];

        if (sCount.toString() === pCount.toString()) {
            ans.push(i + 1);
        }
    }

    return ans;
};
```

# 第五题：和为k的子数组

### **题目内容**：

给你一个整数数组 nums 和一个整数 k ，请你统计并返回 该数组中和为 k 的子数组的个数 。

子数组是数组中元素的连续非空序列。

**示例 1：**

输入：nums = \\[1,1,1\\], k = 2

输出：2

### **题解：**

利用前缀和+哈希表(前缀和，次数)算法求解，求子数组公式为sum(i,j)=preSum(j)-preSum(i-1)=k，j>i,公式移项为preSum(i-1)=k-preSum(j),在当前遍历时，看哈希表中是否存在preSum(i-1)也就是k-preSum(j)。如果存在，ans++。而后，将map(preSum,++);

```
/**
 * @param {number[]} nums
 * @param {number} k
 * @return {number}
 */
var subarraySum = function(nums, k) {
    let map=new Map();
    let preSum=0;
    let ans=0;
    map.set(0,1);
    for(const num of nums){
        preSum+=num;
        if(map.has(preSum-k))
            ans+=map.get(preSum-k);
        map.set(preSum,(map.get(preSum)||0)+1);
    }

    return ans;
};
/**
 * @param {number[]} nums
 * @param {number} k
 * @return {number}
 */
var subarraySum = function(nums, k) {
    let map=new Map();
    let preSum=0;
    let ans=0;
    map.set(0,1);
    for(const num of nums){
        preSum+=num;
        if(map.has(preSum-k))
            ans+=map.get(preSum-k);
        map.set(preSum,(map.get(preSum)||0)+1);
    }

    return ans;
};
```' WHERE id = 19;

-- 22: 闭包详解
UPDATE articles SET content = '**闭包：**闭包意思是有权访问另一个函数作用域中的变量的函数。简单来说，就是当内部函数引用了外部函数中的变量，并且这个内部函数被传递到了外面，这就形成了闭包。那么即使外部函数执行完毕了，因为闭包的存在，这些变量也不会被垃圾回收机制处理，而是继续保存在内存当中。

举一个例子：

**防抖函数**:事件触发后，等待n秒后再次执行。如果中途再次触发事件，则重新计时。

```
function debounce(fn,delay){
  let timer=null;
  return function(...args){
   if(timer) clearTimeout(timer);
   timer=setTimeout(
   ()=>fn.apply(this,args)
   ,delay)
  }
}
```

**节流函数**：不管事件触发的多么频繁，在一定时间内只触发一次。

```
function throttle(fn,delay){
   let timer=null;
   return function(...args){
     if(timer) return;
     timer=setTimeout(()=>{
     fn.apply(this,args);
     timer=null
     }
     ,delay);
   }
}
```

使用例子：

```
 const inputDebounce = document.getElementById(''input-debounce'');
  function sendRequest(type) {

            debounceCount++;
            countDebounceSpan.innerText = debounceCount;
            console.log(''防抖请求发送 SUCCESS!'');

    }
     const debouncedHandler = debounce(() => sendRequest(''debounce''), 500);
    inputDebounce.addEventListener(''input'', debouncedHandler);

```' WHERE id = 22;

-- 27: 虚拟列表
UPDATE articles SET content = '在日常的前端开发中，我们经常会遇到长列表渲染的场景。比如在类似 ChatGPT 的 AIGC 对话平台中 ，对话记录可能会非常长。如果一次性将成百上千条 DOM 节点全部渲染到页面上，会极大地消耗浏览器的内存，导致页面卡顿甚至崩溃。

**为了解决这个问题，虚拟列表（Virtual Scroller） 应运而生。它的核心思想是：无论列表有多长，只渲染当前可视区域及其上下少量缓冲区的 DOM 节点** 。

如果是固定高度的列表，计算非常简单。但实际业务中（比如聊天消息），每一条内容的长短不一，高度是动态的。今天，我们就来深度拆解一下**不定高虚拟列表**的实现原理。

### 不定高虚拟列表的核心实现原理

不定高虚拟列表的难点在于：在我们真正把节点渲染到屏幕上之前，我们是不知道它实际有多高的。因此，我们需要采用“先预估，后修正”的策略。整体实现可以分为以下三个关键步骤：

#### 1\\. 预估每一个列表项的高度，初始化位置缓存

由于无法提前知道真实高度，我们需要先给每一项设定一个**预估高度**（比如默认 50px）。
在此基础上，我们需要维护一个**位置缓存数组（positions）**，用来记录每一个列表项的索引（index）、预估高度（height）、距离顶部的位置（top）以及底部距离（bottom）。

```code-container
// 位置缓存数据结构示例
let positions = [
  { index: 0, height: 50, top: 0, bottom: 50 },
  { index: 1, height: 50, top: 50, bottom: 100 },
  { index: 2, height: 50, top: 100, bottom: 150 },
  // ...
]
```

这个缓存数组是我们后续计算的核心依据。

#### 2\\. 根据预估值，计算虚拟占位框的高度（撑开滚动条）

虽然我们只渲染可视区域的几十个 DOM 节点，但为了让浏览器的滚动条能够正常显示并精确拖动，我们需要一个“幽灵占位框”（Placeholder）。

这个占位框的高度怎么来？很简单，就是取我们`**位置缓存数组中最后一项的 bottom 值**`。将其设置为撑开滚动容器的总高度。这样，视觉上用户会觉得所有数据都已经加载完毕了。

#### 3\\. 监听滚动更新：修正实际值并进行视图偏移

这是最核心的一步，分为两个维度的更新：

-   截取可视数据： 当用户滚动列表时，我们会监听到 scroll 事件，拿到当前的 scrollTop（滚动卷去的高度）。通过对比 scrollTop 和我们缓存数组中的 bottom 值，我们可以快速计算出当前可视区域的起始索引（startIndex）和结束索引（endIndex），从而从源数据中截取出当前需要渲染的那部分数据 。
-   修正缓存并重新排版： 当这部分数据被真实渲染到 DOM 树后（可以在 Vue 的 updated 钩子或使用 ResizeObserver 中获取），我们就能拿到它们真实的 DOM 高度。我们将真实高度与之前的预估高度进行对比，求出差值（d-value）。根据这个差值，循环更新当前项之后所有列表项的 top 和 bottom 实际值。
-   利用 Transform 保持视图稳定： 为了让截取出来的 DOM 节点始终停留在用户的可视范围内，我们需要利用 CSS 的 transform: translateY 属性，将这批节点向下偏移对应的距离 ，偏移量通常是 startIndex 对应项的 top 值。' WHERE id = 27;

-- 29: 全栈博客从零到上线
UPDATE articles SET content = '# 从零到上线：Vue3 + Node.js 全栈博客部署经历

本文记录我在本地开发完成的 Vue3 + Node.js 博客项目部署到阿里云服务器（宝塔面板）的完整操作流程，并总结了在环境配置、数据库权限以及 Nginx 反向代理等方面遇到的实际报错与解决步骤，供初次部署全栈项目的开发者参考。

**技术栈与运行环境：**

-   服务器：阿里云轻量应用服务器（CentOS）
-   运维面板：宝塔面板
-   前端：Vue 3 + TypeScript + Vite
-   后端：Node.js + Express/Koa + Sequelize
-   数据库：MySQL 5.7

* * *

## 第一部分：完整部署流程

### 一、 服务器环境准备

1.  登录宝塔面板，在“软件商店”中安装基础环境：Nginx 1.28、MySQL 5.7。
2.  安装“Node.js版本管理器”（官方推荐，替代旧版 PM2 管理器）。
3.  在 Node.js 版本管理器中，选择并安装 Node.js v20.x 稳定版，并将其设置为命令行默认版本。

### 二、 数据库配置

1.  在宝塔面板“数据库”模块，点击“添加数据库”，设置数据库名称（如 ro-blog）、用户名及密码。
2.  记录生成的数据库连接信息。
3.  通过面板进入 phpMyAdmin，将本地导出的 SQL 结构与数据文件导入至服务器数据库中。

### 三、 后端服务 (Node.js) 部署

1.  将本地后端代码（剔除 node\\_modules 文件夹）压缩打包，上传至服务器指定目录（如 /www/wwwroot/blog-back）并解压。
2.  在后端根目录下新建 .env 环境变量文件，填入线上的数据库主机IP、名称、用户名和密码。
3.  在宝塔面板“网站”菜单下切换至“Node项目”，点击“添加Node项目”。
4.  填写项目信息：选择项目所在目录。设置启动选项为“自定义命令”，输入 node app.js（或对应的启动文件）。填写与代码中一致的项目监听端口（如 5000）。
5.  提交后，宝塔会自动执行依赖安装并启动项目守护进程。
6.  分别进入阿里云控制台防火墙与宝塔面板安全设置，添加 TCP 协议的 5000 端口放行规则。

### 四、 前端静态页面 (Vue 3) 部署

1.  在本地前端项目中，修改接口请求的基础路径（Base URL），由本地 localhost 指向线上的接口地址（或配置为相对路径 /api 配合后续反向代理）。
2.  在本地终端执行打包命令 npm run build，生成 dist 静态资源目录，并将该目录下的文件压缩。
3.  在宝塔面板“网站”菜单下切换至“PHP项目”，点击“添加站点”。在域名栏填入已备案的域名（或服务器公网 IP），PHP 版本选择“纯静态”。
4.  进入该站点绑定的根目录，删除默认生成的无用文件，上传并解压前端打包好的静态文件，确保 index.html 位于根目录下。
5.  在该站点的“设置”页面完成 Nginx 相关配置（包括伪静态与反向代理，具体配置见下方复盘部分），保存后强制刷新浏览器即可访问。

* * *

## 第二部分：部署时出现的问题和解决办法

### 一、 Node.js 后端服务异常

**1\\. pm2 命令未找到**

-   报错：终端提示 -bash: pm2: command not found
-   原因与解决：早期尝试在终端直接使用 pm2 命令运行项目。后改用宝塔面板的“Node.js版本管理器”添加项目，系统会在底层自动接管 pm2 进程守护，无需手动在终端执行。

**2\\. 模块查找失败（启动路径错误）**

-   报错：Error: Cannot find module ''/www/wwwroot/blog-back/src/app.js''
-   原因与解决：宝塔面板默认读取了 package.json 中的旧启动脚本，而实际的启动文件 app.js 位于项目根目录，并非在 src 目录下。在宝塔 Node 项目设置中，将启动选项修改为自定义命令 node app.js 即可解决。

**3\\. Sequelize 无法连接数据库**

-   报错：Dialect needs to be explicitly supplied
-   原因与解决：本地开发时数据库配置存放在 .env 文件中，打包上传时该文件被略过，导致服务器端 Node.js 读取不到配置参数。在宝塔文件管理中手动补齐 .env 文件并填入参数后，重启服务恢复正常。

### 二、 MySQL 数据库权限限制

**1\\. phpMyAdmin 提示权限拒绝**

-   报错：SELECT command denied to user ''ro-blog''@''localhost'' for table ''user''
-   原因与解决：通过宝塔面板快捷点击“管理”进入 phpMyAdmin 时，系统默认使用了新创建的普通用户 ro-blog 登录。由于 phpMyAdmin 初始化需要查询系统的全局 user 表，普通用户权限不足导致报错。加上浏览器缓存了该登录状态，导致刷新后持续报错。
-   解决操作：使用浏览器的无痕/隐私模式，复制宝塔面板提供的 phpMyAdmin 独立安全链接，手动输入 root 账号及对应的管理员密码登录，即可正常管理所有数据库表结构。

### 三、 前端部署与 Nginx 路由配置

**1\\. 前端路由刷新报 404**

-   报错：刷新 Vue 页面显示 404 Not Found (nginx)
-   原因与解决：Vue Router 使用了 History 模式，直接访问非根路径会导致 Nginx 在物理路径中寻找同名文件夹。进入宝塔前端站点的 Nginx 配置文件，找到 location / 代码块，补充 Vue 的防 404 伪静态规则：

```code
location / {
    try_files $uri $uri/ /index.html;
}
```

**2\\. 接口请求返回 404（反向代理失效）**

-   报错：浏览器请求 /api/... 路径返回 404
-   原因与解决：本地开发时的 Vite proxy 代理配置在执行 build 打包后失效。需要在宝塔对应前端站点的“反向代理”设置中添加配置：将“代理目录”设置为 /api，“目标 URL”设置为后端的实际运行地址（如 http://127.0.0.1:5000），交由 Nginx 处理接口跨域与请求转发。

### 四、 域名备案要求

-   问题：配置真实域名访问时被拦截，提示 ERR\\_CONNECTION\\_RESET。
-   说明：国内地域的服务器，其 80 (HTTP) 和 443 (HTTPS) 端口必须在完成工信部 ICP 备案后才能通过域名正常对外提供网页访问。
-   过渡方案：在备案审核期间，通过在宝塔新建站点并绑定“服务器公网 IP + 非80端口（如 8080）”，并在双端防火墙放行该端口，用于临时访问和前端调试。' WHERE id = 29;

-- 30:  Vue3 博客集成 Live2D 角色
UPDATE articles SET content = '# Vue3 博客集成 Live2D 角色

## 前言

这篇记录一下我在当前博客项目里接入 Live2D 角色的完整过程。

目标效果：桌面端显示看板娘，移动端不加载，路由切换不重复初始化。

### **一、我的项目结构**

将文件放到项目里这两个位置：

-   \\`/public/live2d\\`：放模型运行资源
-   \\`/src/App.vue\\`：全局入口，做脚本懒加载与初始化

\\`public\\` 目录下的资源会原样打包到线上根路径，所以看板娘资源访问路径可以写成：

-   \\`/live2d/waifu.css\\`
-   \\`/live2d/live2d-sdk.js\\`
-   \\`/live2d/Core/live2dcubismcore.min.js\\`
-   \\`/live2d/waifu-tips.js\\`
-   \\`/live2d/Resources/...\\`

### 二、准备资源文件

把moc3模型资源放到：\\`/public/live2d\\`

至少要有这些文件（我项目里就是这么用的）：

-   \\`waifu.css\\`
-   \\`waifu-tips.js\\`
-   \\`waifu-tips.json\\`
-   \\`live2d-sdk.js\\`
-   \\`Core/live2dcubismcore.min.js\\`
-   \\`Resources/model\\_list.json\\`
-   \\`Resources/model/...\\`（模型本体）

相应资源可在底部链接获取。

### 三、在 App.vue 做一次性初始化

在 \\`ro-blog/src/App.vue\\` 里做三件事：

1\\. 动态加载外部 css/js（防止阻塞首屏）

2\\. 只初始化一次（避免路由切换重复创建）

3\\. 小屏不加载（移动端性能优先）

代码使用示例：

```
<script setup lang="ts">
import { onMounted, watch } from ''vue''
import { useRoute } from ''vue-router''

const LIVE2D_ROOT = ''/live2d''
const route = useRoute()

declare global {
  interface Window {
    initWidget?: (config: {
      waifuPath: string
      cdnPath: string
      tools?: string[]
      dragEnable?: boolean
      dragDirection?: Array<''x'' | ''y''>
      switchType?: ''order'' | ''random''
    }) => void
    __live2dWidgetInited?: boolean
  }
}

const loadExternalResource = (url: string, type: ''css'' | ''js'') => {
  return new Promise<void>((resolve, reject) => {
    if (type === ''css'') {
      const exists = document.querySelector(`link[href="${url}"]`)
      if (exists) return resolve()
      const link = document.createElement(''link'')
      link.rel = ''stylesheet''
      link.href = url
      link.onload = () => resolve()
      link.onerror = () => reject(new Error(`加载样式失败: ${url}`))
      document.head.appendChild(link)
      return
    }

    const exists = document.querySelector(`script[src="${url}"]`)
    if (exists) return resolve()
    const script = document.createElement(''script'')
    script.src = url
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`加载脚本失败: ${url}`))
    document.body.appendChild(script)
  })
}

const initLive2dWidget = async () => {
  if (window.__live2dWidgetInited) return
  if (window.innerWidth < 768) return

  await Promise.all([
    loadExternalResource(`${LIVE2D_ROOT}/waifu.css`, ''css''),
    loadExternalResource(`${LIVE2D_ROOT}/Core/live2dcubismcore.min.js`, ''js''),
    loadExternalResource(`${LIVE2D_ROOT}/live2d-sdk.js`, ''js''),
    loadExternalResource(`${LIVE2D_ROOT}/waifu-tips.js`, ''js''),
  ])

  if (typeof window.initWidget !== ''function'') return

  window.initWidget({
    waifuPath: `${LIVE2D_ROOT}/waifu-tips.json`,
    cdnPath: `${LIVE2D_ROOT}/Resources/`,
    tools: [],
    dragEnable: false,
    switchType: ''order'',
  })

  window.__live2dWidgetInited = true
}

const updateLive2dVisibility = () => {
  // 当前项目里我默认不隐藏，预留路由隐藏扩展
  const shouldHide = false
  const waifu = document.getElementById(''waifu'')
  const waifuToggle = document.getElementById(''waifu-toggle'')
  if (waifu) waifu.style.display = shouldHide ? ''none'' : ''''
  if (waifuToggle) waifuToggle.style.display = shouldHide ? ''none'' : ''''
}

onMounted(() => {
  initLive2dWidget()
  updateLive2dVisibility()
})

watch(
  () => route.path,
  () => updateLive2dVisibility(),
)
</script>
```

参考教程： [https://blog.dogxi.me/diy-website-live2d](https://blog.dogxi.me/diy-website-live2d)' WHERE id = 30;

-- 32: 滑动窗口最大值（Java实现）
UPDATE articles SET content = '# 滑动窗口最大值

## 问题描述

给定一个数组 `nums` 和一个滑动窗口的大小 `k`，请找出所有滑动窗口里的最大值。

**示例：**

```
输入：nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
输出：[3, 3, 5, 5, 6, 7]
```

解释： | 窗口位置 | 最大值 | |---------|:-----:| | \\[1 3 -1\\] -3 5 3 6 7 | 3 | | 1 \\[3 -1 -3\\] 5 3 6 7 | 3 | | 1 3 \\[-1 -3 5\\] 3 6 7 | 5 | | 1 3 -1 \\[-3 5 3\\] 6 7 | 5 | | 1 3 -1 -3 \\[5 3 6\\] 7 | 6 | | 1 3 -1 -3 5 \\[3 6 7\\] | 7 |

* * *

## 解法一：暴力法

最直观的思路，遍历每个窗口，在每个窗口内找最大值。

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    if (nums == null || nums.length == 0) return new int[0];
    int n = nums.length;
    int[] result = new int[n - k + 1];

    for (int i = 0; i <= n - k; i++) {
        int max = nums[i];
        for (int j = i; j < i + k; j++) {
            max = Math.max(max, nums[j]);
        }
        result[i] = max;
    }
    return result;
}
```

-   ⏱ **时间复杂度：** O(n × k)
-   💾 **空间复杂度：** O(1)

* * *

## 解法二：双端队列（最优解）

使用一个双端队列（Deque）来维护窗口内可能成为最大值的元素下标。

**核心思路：**

1.  队列中始终保持**递减顺序**，队首永远是当前窗口的最大值
2.  遍历数组时，移除队尾所有比当前元素小的元素
3.  将当前元素下标加入队尾
4.  当队首下标移出窗口时，将其移除
5.  当窗口形成后（i >= k-1），记录队首元素

```java
import java.util.*;

public int[] maxSlidingWindow(int[] nums, int k) {
    if (nums == null || nums.length == 0) return new int[0];
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new LinkedList<>(); // 存储下标

    for (int i = 0; i < n; i++) {
        // 1. 移除队尾所有比当前元素小的元素
        while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
            deque.pollLast();
        }

        // 2. 将当前元素下标加入队尾
        deque.offerLast(i);

        // 3. 移除已经滑出窗口的队首元素
        if (deque.peekFirst() < i - k + 1) {
            deque.pollFirst();
        }

        // 4. 窗口形成后，记录最大值
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

-   ⏱ **时间复杂度：** O(n) —— 每个元素最多入队和出队一次
-   💾 **空间复杂度：** O(k) —— 队列最多存储 k 个元素

* * *

## 测试代码

```java
public static void main(String[] args) {
    int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
    int k = 3;
    int[] result = maxSlidingWindow(nums, k);
    System.out.println(Arrays.toString(result));
    // 输出：[3, 3, 5, 5, 6, 7]
}
```

* * *

## 总结

解法

时间复杂度

空间复杂度

适用场景

暴力法

O(n×k)

O(1)

小规模数据

双端队列

O(n)

O(k)

大规模数据，推荐使用

**双端队列法**是解决滑动窗口最大值问题的最经典解法，面试中也是高频考点，建议熟练掌握～😊' WHERE id = 32;

-- 33: 最小覆盖子串（JavaScript实现）
UPDATE articles SET content = '## 题目描述

给你两个字符串 `s` 和 `t`，返回 `s` 中的 **最短窗口** 子串，使得该子串包含 `t` 中的每一个字符（包括重复字符）。如果没有这样的子串，返回空字符串 `""`。

测试用例保证答案唯一。

**示例 1：**

```
输入：s = "ADOBECODEBANC", t = "ABC"
输出："BANC"
解释：最小覆盖子串 "BANC" 包含来自字符串 t 的 ''A''、''B'' 和 ''C''。
```

## 解题思路

这道题是滑动窗口的经典应用，核心思路：

1.  **用哈希表记录 t 中每个字符的需求量**
2.  **右指针扩展窗口**，找到包含 t 所有字符的子串
3.  **左指针收缩窗口**，尝试找到更短的可行解
4.  用一个变量记录窗口中还差多少种字符满足条件

## 代码实现

```javascript
/**
 * @param {string} s
 * @param {string} t
 * @return {string}
 */
var minWindow = function(s, t) {
  if(s.length < t.length) return '''';

  let needMap = {};
  for(const ch of t) needMap[ch] = (needMap[ch] || 0) + 1;

  let right = 0, left = 0;
  let cntype = Object.keys(needMap).length;
  let minLen = 100000;
  let ans = '''';

  while(right < s.length) {
    if(s[right] in needMap) {
      needMap[s[right]]--;
      if(needMap[s[right]] == 0) cntype--;
    }
    right++;

    while(cntype == 0) {
      if(right - left < minLen) {
        minLen = right - left;
        ans = s.slice(left, right);
      }
      if(s[left] in needMap) {
        needMap[s[left]]++;
        if(needMap[s[left]] > 0) cntype++;
      }
      left++;
    }
  }

  return ans;
};
```

## 代码逐行解析

步骤

说明

`needMap`

记录 t 中每个字符还需要多少个

`cntype`

记录还有多少种字符未满足，为 0 时说明当前窗口已包含所有所需字符

右指针扩展

遇到 needMap 中的字符，需求量减一，若减到 0 则 cntype 减一

左指针收缩

当 cntype == 0 时，尝试缩小窗口，记录最小长度

窗口收缩

左指针字符若是 needMap 中的，需求量加一，若从 0 变正数则 cntype 加一

## 示例运行过程

以 `s = "ADOBECODEBANC"`, `t = "ABC"` 为例：

窗口

是否包含

说明

\\[A D O B E C\\]

✅ 包含 ABC

right=6，cntype=0，记录 "ADOBEC"

\\[D O B E C\\]

❌ 缺少 A

left++，cntype>0，继续扩展

...

...

...

\\[B E C O D E B A N C\\]

✅

右指针到末尾，最终找到 "BANC" 最短

## 复杂度分析

-   **时间复杂度：** O(m + n) —— 左右指针各遍历一次 s，m = s.length
-   **空间复杂度：** O(n) —— needMap 存储 t 中字符，n = t.length' WHERE id = 33;

-- 34: 经典算法：最大子数组和（LeetCode 53）
UPDATE articles SET content = '# 经典算法：最大子数组和

## 问题描述

给你一个整数数组 `nums` ，请你找出一个**具有最大和的连续子数组**（子数组最少包含一个元素），返回其最大和。

**子数组**是数组中的一个连续部分。

### 示例

> 输入：nums = \\[-2,1,-3,4,-1,2,1,-5,4\\] 输出：6 解释：连续子数组 \\[4,-1,2,1\\] 的和最大，为 6。

* * *

## 思路分析

这道题是动态规划的经典入门题，核心思路是 **Kadane 算法**：

遍历数组时，维护两个变量：

-   `pre`：以当前元素结尾的连续子数组的最大和
-   `maxAns`：全局最大和

**状态转移方程**：

```
pre = Math.max(pre + x, x)
```

什么意思呢？对于每个元素 `x`，有两种选择：

1.  把 `x` 加到前面的子数组上 → `pre + x`
2.  从 `x` 重新开始一个新的子数组 → `x`

取两者中较大的一个作为新的 `pre`，然后更新全局最大值。

* * *

## 代码实现

```javascript
/**
 * @param {number[]} nums
 * @return {number}
 */
var maxSubArray = function(nums) {
    let pre = 0, maxAns = nums[0];
    nums.forEach((x) => {
        pre = Math.max(pre + x, x);
        maxAns = Math.max(maxAns, pre);
    });
    return maxAns;
};
```

### 复杂度分析

-   **时间复杂度**：O(n) — 只需遍历一次数组
-   **空间复杂度**：O(1) — 只使用了常数个变量

* * *

## 手动模拟

以 `nums = [-2, 1, -3, 4, -1, 2, 1, -5, 4]` 为例：

元素 x

pre（以 x 结尾的最大和）

maxAns（全局最大）

\\-2

max(0-2, -2) = -2

\\-2

1

max(-2+1, 1) = 1

1

\\-3

max(1-3, -3) = -2

1

4

max(-2+4, 4) = 4

4

\\-1

max(4-1, -1) = 3

4

2

max(3+2, 2) = 5

5

1

max(5+1, 1) = 6

6

\\-5

max(6-5, -5) = 1

6

4

max(1+4, 4) = 5

6

最终得到最大和 **6**，对应子数组 `[4, -1, 2, 1]` ✅

* * *

## 总结

-   Kadane 算法的精髓在于「**要么续前缘，要么从头来**」
-   边界处理：当 `nums` 只有一项时，直接返回该值
-   如果要求**返回子数组本身**，可以在更新 `pre` 时记录起始位置' WHERE id = 34;

-- 35: LeetCode 189. 轮转数组 — 三次反转与辅助数组解法全解
UPDATE articles SET content = '# LeetCode 189. 轮转数组

## 题目描述

给定一个整数数组 `nums`，将数组中的元素**向右轮转** `k` 个位置，其中 `k` 是非负数。

### 示例

**示例 1：**

```
输入：nums = [1,2,3,4,5,6,7], k = 3
输出：[5,6,7,1,2,3,4]
解释：
向右轮转 1 步：[7,1,2,3,4,5,6]
向右轮转 2 步：[6,7,1,2,3,4,5]
向右轮转 3 步：[5,6,7,1,2,3,4]
```

**示例 2：**

```
输入：nums = [-1,-100,3,99], k = 2
输出：[3,99,-1,-100]
解释：
向右轮转 1 步：[99,-1,-100,3]
向右轮转 2 步：[3,99,-1,-100]
```

* * *

## 解法一：使用额外数组（辅助数组）

这是最直观的解法。核心思路是：**每个元素的新位置 = (原位置 + k) % n**。

### 代码实现

```javascript
/**
 * @param {number[]} nums
 * @param {number} k
 * @return {void} Do not return anything, modify nums in-place instead.
 */
var rotate = function(nums, k) {
    const n = nums.length;
    const newArr = new Array(n);

    for (let i = 0; i < n; i++) {
        newArr[(i + k) % n] = nums[i];
    }

    for (let i = 0; i < n; i++) {
        nums[i] = newArr[i];
    }
};
```

### 详解

1.  **确定数组长度** `n`
2.  **计算每个元素的新位置**
3.  **将元素放入新数组对应位置**
4.  **将新数组复制回原数组**（题目要求原地修改）

> 💡 取模 % n 是关键，它让索引在超出数组长度时自动回到开头。

### 时间复杂度 & 空间复杂度

-   **时间复杂度：O(n)** — 遍历数组两次
-   **空间复杂度：O(n)** — 使用了额外数组

* * *

## 解法二：三次反转（空间 O(1) 进阶）

既然要求原地修改，能不能不用额外数组呢？当然可以！利用**数组反转**的技巧：

### 思路

1.  将整个数组反转
2.  将前 `k` 个元素反转
3.  将剩余 `n - k` 个元素反转

### 代码实现

```javascript
var rotate = function(nums, k) {
    const n = nums.length;
    k = k % n; // 处理 k > n 的情况

    // 辅助反转函数
    const reverse = (arr, start, end) => {
        while (start < end) {
            [arr[start], arr[end]] = [arr[end], arr[start]];
            start++;
            end--;
        }
    };

    // 1. 反转整个数组
    reverse(nums, 0, n - 1);
    // 2. 反转前 k 个
    reverse(nums, 0, k - 1);
    // 3. 反转后 n-k 个
    reverse(nums, k, n - 1);
};
```

### 图解示例

以 `nums = [1,2,3,4,5,6,7], k = 3` 为例：

步骤

操作

数组状态

初始

—

`[1,2,3,4,5,6,7]`

①

反转整个数组

`[7,6,5,4,3,2,1]`

②

反转前 k=3 个

`[5,6,7,4,3,2,1]`

③

反转后 n-k=4 个

`[5,6,7,1,2,3,4]` ✅

### 为什么这样可行？

数学上可以证明：

-   原数组索引 `i` 的元素最终要去 `(i + k) % n` 位置
-   三次反转后，元素恰好完成这个映射

### 时间复杂度 & 空间复杂度

-   **时间复杂度：O(n)** — 每个元素被反转两次
-   **空间复杂度：O(1)** — 只使用了常数级额外空间

* * *

## 注意事项

1.  `**当 k > n 时**`，取模 `k = k % n`，因为轮转 `n` 次等于没转
2.  题目要求**原地修改**，不能直接返回新数组
3.  注意 `k = 0` 或 `n = 0` 的边界情况

## 总结

解法

时间复杂度

空间复杂度

特点

辅助数组

O(n)

O(n)

直观易理解

三次反转

O(n)

O(1)

空间最优，面试常考

推荐掌握**三次反转**解法，既省空间又能体现对数组操作的熟练度喵~' WHERE id = 35;

-- 54: 面试准备
UPDATE articles SET content = '### 1.面试实战：如果面试官问“你简历里说的避免脏读是怎么回事？”

你可以这样回答，直接展现你的高级工程思维：

> “在我们的制造培训系统中，工位执行屏对数据的实时性和准确性要求极高。一开始我们在做 WebSocket 推送时发现一个隐患：如果业务逻辑还在执行事务中，就直接向前端推送状态，一旦后续逻辑报错导致事务回滚，前端屏幕就会显示错误的状态，也就是业务上的‘脏读’。 为了解决这个问题，我利用了 Spring 事务的生命周期机制。我将 WebSocket 的消息广播逻辑绑定在了事务的 afterCommit 阶段。也就是说，只有当工单或物料状态的数据百分之百落库成功后，后端才会按工位向前端长连接推送最新的执行上下文。这样既保证了工序、节拍状态的近实时同步，又从底层逻辑上切断了半提交推送的风险。”' WHERE id = 54;

-- 56: 面试准备---项目和实习
UPDATE articles SET content = '## 一、基础信息 & 教育背景

**Q1：简单做个自我介绍吧。**

> 面试官你好，我叫贾鑫科，我是来自华北水利水电大学的大三学生，在今年的3到5月我在无锡零可达科技有限公司进行全栈的实习。实习期间使用springcloud和satoken实现跨服务的sso单点登录，在车间中拧紧机的产生的高频数据，用java的nio和rabbitMQ来实现的这个数据读取和写入。在项目方面，完成了一个aiagent知识库系统，结成langgraph实现的这个多agent编排。，并且还实现了这个rag语义检索和mcp工具扩展。

**Q2：学到什么。**

> 要说提升最大的，面试官，这段实习和项目经历让我在三个层面收获非常大：
> 第一，对SSO单点登录和分布式会话的理解从“会用”变成了“懂原理”。
> 以前在学校做项目，登录就是用Session存在单机里，根本没考虑过多服务共享的问题。到了真实微服务环境才发现，用户登录了A系统，访问B系统时身份就丢了，因为Session不共享。
> 通过这次实践，我真正理解了SSO的核心本质——就是“统一认证中心 + 集中会话存储”。用户登录后，认证中心下发Token，所有子系统拿着这个Token去同一个Redis里校验身份，这样就实现了“一次登录，通行所有服务”。同时我也认识到，分布式环境下不能依赖Servlet容器自带的Session，必须把会话状态抽离到外部存储（Redis），这样才能保证服务无状态、水平扩展时用户不掉线。

### 二、实习经历（无锡零可达科技——全栈开发）

#### 1\\. 微服务权限治理与认证穿透

**问：你在微服务权限治理中具体做了什么？**

> 我们MES项目采用Spring Cloud + Vue3的微服务架构，有十几个子系统。我的任务是解决两个问题：一是用户一次登录后跨服务访问时身份丢失，二是服务间调用时Token透传的安全问题。我引入了Sa-Token作为统一认证框架，基于Redis集中管理分布式会话，实现了SSO单点登录。然后我自定义了Feign请求拦截器，在每次服务间调用时自动从当前上下文中取出JWT Token，注入到请求头中，下游服务再通过过滤器解析并校验权限。同时我设计了动态RBAC权限体系，将用户角色与接口资源映射关系缓存到Redis，每次请求实时鉴权。

#### 2\\. 工业物联数据监听采集与时序存储

**问：拧紧机数据采集这个场景你怎么做的？**

> 零部件再制造车间有拧紧机，它会持续产生高频的拧紧曲线数据，文件会不断写入到指定目录。我们的需求是实时采集并存储，供后续质量分析。我采用了Java NIO的WatchService对目录进行毫秒级监听，一旦有新文件生成立即触发读取。但高频写入时容易读到半包文件，所以我设计了双重校验防抖机制：先检查文件大小在连续两次检测中是否稳定，再校验文件头尾标识，只有完整文件才会被处理。采集到的数据量很大，直接写入数据库会打崩系统。我用RabbitMQ做消息队列进行削峰，异步批量消费，然后存入InfluxDB时序数据库。InfluxDB针对时间序列场景做了优化，区间采样查询性能比MySQL提升了近10倍，前端拉取拧紧曲线时响应很快。

#### 4\\. 为什么用InfluxDB存储拧紧曲线，不用MySQL？

> 拧紧曲线是典型的时序数据，每秒产生数十个采样点，数据体量极大，且核心查询场景是按时间区间拉取单段拧紧过程的曲线。 InfluxDB作为专业时序数据库，针对时间戳做了专项索引优化，时间范围查询性能远高于MySQL；同时数据压缩率更高，同等数据量占用存储空间更小，非常适合海量工业采样数据的存储。如果使用MySQL存储，百万级采样数据后区间查询延迟会非常高，无法满足实时展示的业务要求。

#### 3\\. 高性能大表渲染与工业拓扑可视化

**问：百万级物料库存盘点页面你是怎么优化的？**

> 前端需要展示上万条库存记录，直接渲染会导致页面卡死。我基于vxe-table深度封装了虚拟滚动组件，只渲染可视区域的行，同时结合异步分块加载——每次滚动到底部时请求下一批数据，而不是一次性加载全部。优化后，首屏渲染耗时从原来的3秒多降到了200毫秒以内，FPS稳定在50+，用户体验非常流畅。另外，我还用AntV X6图编辑引擎开发了工序流转和AGV调度路径图。我实现了节点拖拽、连线合法性校验（比如不允许闭环或跨层级连接），并将图数据序列化为JSON，通过接口同步到后端持久化。这样业务人员可以直接在界面上调整产线布局，系统自动保存状态。

### 三、项目经历（基于AI Agent的智能知识库系统）

#### 1\\. 自动化CI/CD与多容器编排

**问：你的CI/CD流水线是怎么设计的？**

> 这个项目我独自完成，从开发到部署都自己搞定。我在GitHub上维护代码，利用GitHub Actions构建自动化流水线：每当代码push到main分支，Actions自动触发构建，将前端、后端分别打包成Docker镜像，推送到ghcr容器仓库，然后通过ssh登录云服务器，执行docker-compose pull和up -d，实现零停机的滚动更新——因为我的compose文件里配置了重启策略和健康检查，旧容器会等新容器就绪后才停止。这套流程让我每次迭代都能快速上线，而且回滚也很方便，只需重新部署上一个镜像tag。

#### 2\\. 多Agent协作工作流（LangGraph）

**问：你的多Agent是怎么协作的？**

> 我基于LangGraph构建了三类Agent：Planner（规划者）、Executor（执行者）、Reviewer（审核者）。用户提出一个复杂任务后，Planner会将其拆解为多个子任务，并构建一个有向无环图（DAG），通过拓扑排序识别出依赖关系，然后将无依赖的子任务并行分发给多个Executor实例，以提升效率。每个Executor执行完成后，Reviewer会检查结果是否符合预期，如果不通过会触发重试（最多2次），并记录失败原因。这套机制使得系统能够自主完成多步骤任务，比如“搜集某领域论文摘要并生成综述”，能自动调度搜索、摘要、聚合等步骤，且具备自我纠错能力。

#### 3\\. 有向无环图和拓扑排序算法

> 用户请求进入 Planner Agent 后，由大模型负责任务拆解和依赖分析，输出结构化 DAG JSON。JSON 中每个节点包含任务信息以及 depends\\_on 字段，表示前置任务。后端解析 JSON 后，根据 depends\\_on 构建邻接表和入度表，通过 Kahn 拓扑排序找到当前入度为0的任务执行。没有依赖的任务可以并行分发给不同 Agent，任务完成后更新后续节点状态。

> 我这里采用的是 Kahn 算法。首先根据 DAG 中的依赖关系构建邻接表，同时统计每个任务节点的入度。执行时，将所有入度为0的节点加入队列，因为这些任务没有前置依赖，可以直接执行。每执行完成一个节点，就遍历它的后继节点，将对应节点入度减1，如果后继节点入度变成0，则加入执行队列。循环这个过程，直到所有任务完成。这样可以保证任务按照依赖顺序执行，同时支持多个入度为0的任务并行调度。

#### 4\\. RAG知识检索优化

**问：你的RAG检索怎么做的？**

> 我集成了FAISS向量库和智谱Embedding模型，构建语义搜索引擎。用户上传文档时，系统自动进行HTML/Markdown清洗，分块后生成向量并增量索引。检索时，我采用混合策略：先做语义相似度检索，如果召回结果置信度低，则回退到关键词检索（基于BM25），保证召回率。为了量化检索质量，我引入了RAGAS评估框架，从Faithfulness（忠实性）、Answer Relevance（答案相关性）等四个维度进行评测，并据此调优了分块大小和top-k值。最终检索准确率提升了约15%。

#### 5\\. 增量索引

> 我项目里的增量索引主要是针对知识库文档新增、修改和删除场景设计的。每篇文档会保存 document\\_id、更新时间、内容哈希和索引状态。文档进入索引流程后，我先对清洗后的内容计算哈希，与上一次索引时的哈希进行比较；如果一致就跳过，如果发生变化，就只对当前文档进行重新清洗、切片和 Embedding，而不是重建整个知识库。

> 对新增文档，我会直接生成新的 chunk 和向量并追加到 FAISS；对修改文档，会根据 document\\_id 找到原有 chunk，将旧 chunk 删除或标记失效，再写入新向量；对删除文档，则把对应 chunk 标记为无效，并在检索阶段过滤。FAISS 主要保存向量，chunk 内容、来源、哈希值和状态等元数据保存在 MySQL 中

#### 问：怎么优化海量数据

> 海量文件场景下，我会把耗时的文档解析、切片和向量化提前到索引阶段完成，用户查询时不会再遍历原始文件。查询阶段先根据用户和知识库范围缩小数据范围，再同时进行向量检索和关键词检索。向量检索主要查找语义相近的内容，关键词检索主要补充错误码、编号和专有名词等精确匹配场景。两路结果合并后进行重排，只选少量高相关片段交给大模型。

> 在向量数量较少时可以使用 FAISS Flat 进行精确搜索；向量数量增长后，可以根据压测结果选择 IVF 或 HNSW，通过减少需要比较的向量数量提高检索速度。同时对重复检索结果使用 Redis 缓存，并在知识库更新时通过索引版本让旧缓存失效。生成阶段使用 SSE 流式返回，让用户更早看到答案。

#### 6\\. MCP协议接入

**问：MCP协议你是怎么集成的？**

> 我开发了一个MCP客户端模块，让Agent工作流可以动态加载外部MCP服务，比如Brave Search（实时搜索）、Filesystem（本地文件读写），以及我自己定制的工具（比如数据库查询）。Agent在执行任务时，会根据当前步骤需要的工具类型，通过MCP协议调用对应的服务端点，获得结果后再继续下一步。这样就打破了大模型只能输出文本的限制，让它能真正操作外部系统，显著拓展了能力边界。比如用户问“搜索最近AI新闻并保存到本地”，Agent就能依次调用搜索和文件写入工具完成全流程。

**问：MCP协议你是怎么集成的？**

> 我项目里实现了一个统一的工具注册和路由层。项目启动时，首先加载本地工具，同时根据配置启动或连接 MCP Server。MCP Client 会调用 list\\_tools 动态获取服务端提供的工具名称、描述和输入 Schema，然后将这些工具转换成 Agent 可以识别的 Tool，统一注册到工具中心。

> Agent 产生 Function Calling 后，Executor 会根据工具的 source 字段进行路由。如果 source 是 local，就直接调用项目内部的 Python 函数；如果是 mcp，就通过 MCP Client 调用对应 MCP Server 的工具，并把返回结果写回 LangGraph 状态。

> 本地工具主要用于查询知识库、保存文章等项目内部业务；MCP 工具主要用于 Brave Search、Filesystem 等外部通用能力。

#### 5\\. LLM Tracing与Bad Case反馈闭环

**问：你的失败案例反馈闭环是怎么实现的？**

> 当Reviewer判定某个任务执行失败时，系统会自动将当时的完整上下文（用户输入、Planner拆解、Executor中间结果、Reviewer判定理由）存入MySQL作为“失败案例”。后续Executor执行新任务前，会先对新任务的描述做关键词相似度匹配，检索出历史相近的失败案例，并将其作为“负面示例”动态注入到Prompt中，提醒模型避免同样的错误。这相当于让系统“从失败中学习”，随着案例库积累，模型在相似场景下的成功率会逐渐提高。我目前收集了上百个案例，在实际测试中，重复错误的出现率下降了约30%。' WHERE id = 56;

-- 57: 技术博客大纲示例
UPDATE articles SET content = '# 技术博客大纲模板

## 一、引言

-   问题背景：为什么要写这篇博客？
-   目标读者：适合哪些人群阅读？
-   阅读收益：读者能学到什么？
-   前置知识：需要了解哪些基础知识？

## 二、核心概念

-   基本定义：该技术/工具是什么？
-   核心原理：底层是如何工作的？
-   与其他方案的对比：优势和劣势分析
-   适用场景：什么情况下适合使用？

## 三、环境准备

-   开发工具：IDE、编辑器等推荐
-   依赖安装：需要安装的库/包/工具
-   项目初始化：快速搭建项目骨架
-   基础配置：必要的配置项说明

## 四、实践步骤

### Step 1：基础功能实现

-   核心代码片段
-   关键逻辑说明
-   运行效果展示

### Step 2：进阶功能实现

-   优化与扩展
-   常见坑点提醒
-   性能考量

### Step 3：完整示例

-   项目结构一览
-   核心代码全貌
-   运行结果截图/演示

## 五、最佳实践

-   编码规范建议
-   架构设计原则
-   错误处理策略
-   测试与调试技巧

## 六、常见问题（FAQ）

-   Q1：为什么会出现 XXX 错误？
-   Q2：如何优化性能？
-   Q3：如何与其他工具集成？
-   Q4：生产环境部署注意事项？

## 七、总结

-   核心要点回顾
-   延伸阅读推荐
-   互动讨论引导

## 八、参考资源

-   官方文档链接
-   相关书籍推荐
-   优质社区与博客
-   开源项目地址' WHERE id = 57;

-- 58: Starlore 的 DevOps 实践：从本地 Git Push 到 Docker 容器自动化热更新
UPDATE articles SET content = '# Starlore 的 DevOps 实践：从本地 Git Push 到 Docker 容器自动化热更新

* * *

## 🌟 写在前面

在浩瀚的代码宇宙中，每一次 `git push` 都是一次星舰的跃迁指令。而我们——**Starlore（星旅）** ，追求的不仅是功能的上线，更是让每次部署都如同星辰流转般优雅、稳定与自动化。

这篇文章记录了我为 **Starlore** 搭建 DevOps 自动化部署流水线的完整实战过程，包括架构设计、CI/CD 工作流配置，以及那些在阿里云上跟 ghcr.io 网络"搏斗"的真实踩坑与优化。希望能给同样在云上部署实战项目的你，点亮一颗指路星。

* * *

## 一、项目背景与架构概览

### 1.1 Starlore 是什么？

Starlore 是一个融合了技术博客与星空人文氛围的网站项目，承载着我们对技术与宇宙的双重热爱。

### 1.2 技术栈全景

层级

技术选型

说明

**前端**

Vue 3 + Vite

轻量快速，构建产物仅 ~6.7MB

**后端**

Spring Boot 3 + JWT

提供 RESTful API 与用户鉴权

**PDF 服务**

独立 Node.js 服务（PM2 管理 @ 3001 端口）

专门处理 PDF 生成，避免 Chromium 庞大镜像拖慢构建

**服务器**

阿里云轻量应用服务器（国内）

低成本高性价比的云上运行环境

**容器化**

Docker + docker-compose

统一编排所有服务

### 1.3 为什么单独拆分 PDF 服务？

最初考虑将 PDF 生成能力集成到 Spring Boot 后端容器中，但 Chromium 依赖的镜像大小动辄 1GB+，拉取和构建都极其痛苦。最终决策：

-   **PDF 服务独立部署在宿主机**，使用 PM2 守护进程监听 `3001` 端口
-   后端通过 HTTP 调用 PDF 服务接口
-   这样 Docker 镜像保持轻量化，部署速度大幅提升

* * *

## 二、DevOps 自动化部署流程设计

### 2.1 分支策略

```
Dev（本地开发验证） → 合并到 main → 触发自动部署
```

日常开发在本地 Dev 分支编写代码、本地测试验证，验证通过后合并到 `main` 分支并推送，GitHub Actions 自动接管后续一切。

### 2.2 CI/CD 工具链

-   **代码托管 & CI/CD**：GitHub + GitHub Actions
-   **容器镜像仓库**：GitHub Container Registry (`ghcr.io`)
-   **远程部署**：基于 SSH 的远程命令执行

### 2.3 完整工作流（6 步核心流程）

```
代码推送 (main)
    ↓
① 自动化编译（Java + Vue 并行构建）
    ↓
② Docker 镜像构建 & 推送至 GHCR
    ↓
③ docker-compose.yml 传输至服务器
    ↓
④ SSH 远程执行：docker-compose pull
    ↓
⑤ SSH 远程执行：docker-compose down && up -d
    ↓
⑥ 清理冗余旧镜像：docker image prune -f
    ↓
✨ 部署完成，服务无缝热更新
```

* * *

## 三、核心配置实战解析

### 3.1 工作流文件（`.github/workflows/deploy.yml`）精要

```yaml
name: Deploy Starlore to Aliyun

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - name: 📥 Checkout 代码
        uses: actions/checkout@v4

      - name: 🔧 编译 Java 后端
        run: |
          cd server
          chmod +x mvnw
          ./mvnw clean package -DskipTests

      - name: 🔧 构建 Vue 前端
        run: |
          cd web
          npm install
          npm run build

      - name: 🐳 构建 Docker 镜像并推送至 GHCR
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker compose build
          docker compose push

      - name: 📄 传输 docker-compose.yml 到服务器
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.ALIYUN_HOST }}
          username: ${{ secrets.ALIYUN_USER }}
          key: ${{ secrets.ALIYUN_SSH_KEY }}
          source: "docker-compose.yml"
          target: "/opt/starlore"

      - name: 🚀 SSH 远程更新服务
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.ALIYUN_HOST }}
          username: ${{ secrets.ALIYUN_USER }}
          key: ${{ secrets.ALIYUN_SSH_KEY }}
          command_timeout: 30m   # ⭐ 关键优化点！
          script: |
            cd /opt/starlore
            docker compose pull
            docker compose down
            docker compose up -d
            docker image prune -f
```

### 3.2 关键工具说明

工具

用途

`appleboy/scp-action`

通过 SCP 安全传输配置文件到服务器

`appleboy/ssh-action`

通过 SSH 在远程服务器执行部署命令

* * *

## 四、实战踩坑：当国内服务器直连 ghcr.io

### 4.1 🌪️ 问题发现

一切配置就绪后，满怀期待地推送代码，然后——**等待**。

GitHub Actions 日志里，`docker compose pull` 这一步像被冻住了一样：

```
Run appleboy/ssh-action@v1.0.3
...
  🐳 Pulling starlore-web...
  ⏳ ... 等待 5 分钟 ...
  ⏳ ... 等待 10 分钟 ...
❌ 失败：Run Command Timeout
```

### 4.2 🔍 根因分析

-   **罪魁祸首**：阿里云国内服务器 → ghcr.io 跨境网络不稳定
-   **镜像大小**：前端静态资源层仅 **6.7MB**（理论上秒级拉取）
-   **实际耗时**：经常超过 **10 分钟**，甚至更长
-   **默认超时**：`appleboy/ssh-action` 默认超时为 10 分钟（600 秒）
-   **结果**：网络丢包导致拉取缓慢，超时中断，部署失败

### 4.3 💡 解决方案：延长 SSH 命令超时

在 `appleboy/ssh-action` 步骤中加入 `command_timeout` 参数：

```yaml
- name: 🚀 SSH 远程更新服务
  uses: appleboy/ssh-action@v1.0.3
  with:
    host: ${{ secrets.ALIYUN_HOST }}
    username: ${{ secrets.ALIYUN_USER }}
    key: ${{ secrets.ALIYUN_SSH_KEY }}
    command_timeout: 30m   # ⭐ 从默认10分钟延长至30分钟
    script: |
      cd /opt/starlore
      docker compose pull
      docker compose down
      docker compose up -d
      docker image prune -f
```

### 4.4 ✨ 优化效果

指标

优化前

优化后

超时限制

10 分钟

30 分钟

部署成功率

~60%（经常超时失败）

**100%**

平均部署耗时

不稳定，经常失败

8~15 分钟（耐心等待网络传输）

部署不再因为网络波动而中断，即便 ghcr.io 偶尔"抽风"，流水线也会在后台耐心等待，最终完成热更新。这正是 DevOps 的哲学——**容忍不稳定，但绝不容忍失败**。

* * *

## 五、部署流程全貌（示意图）

```
┌─────────────────────────────────────────────────┐
│                 开发者                            │
│         git push origin main                     │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│           GitHub Actions (CI)                    │
│                                                   │
│  ① Checkout  →  ② 编译后端+前端  →  ③ 构建镜像 │
│                                                   │
│  ④ 推送镜像到 ghcr.io ────────────────────────► │
└──────────────────┬──────────────────────────────┘
                   │
          ┌────────┴────────┐
          ▼                 ▼
   appleboy/scp-action  appleboy/ssh-action
   (传输docker-compose)  (远程执行部署命令)
          │                 │
          ▼                 ▼
┌─────────────────────────────────────────────────┐
│              阿里云轻量服务器                      │
│                                                   │
│  /opt/starlore/                                   │
│    ├── docker-compose.yml                         │
│    ├── starlole-web (容器) ◄── ghcr.io 拉取镜像  │
│    ├── starlore-api (容器)  ◄── ghcr.io 拉取镜像  │
│    └── ...                                        │
│                                                   │
│  🐳 docker-compose pull + down + up -d           │
│  🧹 docker image prune -f                        │
└─────────────────────────────────────────────────┘
```

* * *

## 六、经验总结与星旅精神

### ✅ 最佳实践清单

1.  **镜像轻量化**：将 PDF 等重型服务剥离到宿主机独立管理，避免 Docker 镜像膨胀
2.  **分支策略清晰**：Dev 开发 → main 发布，简单可靠
3.  **超时设置留足余量**：国内服务器访问海外服务，务必测试网络状况并配置合理超时
4.  **静默清理**：`docker image prune -f` 在每次部署后自动清理旧镜像，防止磁盘堆积

### 🌌 Starlore 的星空哲思

在星旅的世界观里，部署不仅仅是一次技术操作——它更像是一场星际补给。代码从本地发射，穿越 GitHub 的星际网关，在 GHCR 的星港短暂停靠补给，最终降落在阿里云的星球表面。

网络延迟是星际通信的固有物理限制，我们无法消除它，但可以用 **command\\_timeout: 30m** 这样的配置，给星际传输留足时间。就像仰望星空时，我们知道那颗星星的光芒穿越了数万年的旅途才抵达我们的眼睛——有些等待，值得耐心守候。

* * *

> 愿你的每次部署，都如星光般顺利抵达。 —— Starlore  🚀✨' WHERE id = 58;

-- 60: Starlore（星旅）DevOps 自动化部署实战：从 GitHub Actions 到阿里云的无缝交付
UPDATE articles SET content = '# Starlore（星旅）DevOps 自动化部署实战：从 GitHub Actions 到阿里云的无缝交付

## 一、项目背景

### 网站简介

**Starlore（星旅）** 是一个以星空旅行为主题的 Web 应用，采用以下技术栈构建：

-   **前端**：Vue 3 + Vite
-   **后端**：Spring Boot 3 + JWT 认证
-   **PDF 服务**：独立 Node.js 服务，使用 PM2 在宿主机 3001 端口管理（规避 Chromium 庞大镜像拉取问题）

### 服务器环境

国内阿里云轻量应用服务器，部署了完整的 Docker 化服务集群。

* * *

## 二、自动化部署流水线设计

### 2.1 分支策略

采用简洁高效的双分支模型：

-   **Dev 分支**：本地开发与自测验证
-   **main 分支**：稳定版本，推送即触发自动化部署

### 2.2 CI/CD 工具链

选用 **GitHub Actions** 作为自动化工作流引擎，由推送到 `main` 分支触发。

### 2.3 核心部署流程

整个流水线分为四个关键步骤，环环相扣：

```
代码推送 → 自动编译 → 镜像推送 → 配置传输 → 远程更新 → 清理旧镜像
```

#### 步骤一：自动化编译

GitHub Runner 自动拉取最新代码，执行多模块编译：

-   **Java 后端**：使用 Maven 打包构建 Spring Boot 3 应用
-   **Vue 前端**：使用 Vite 构建生成静态资源

#### 步骤二：镜像推送至 GHCR

构建好的 Docker 镜像自动推送至 **GitHub Container Registry（ghcr.io）**，确保镜像版本与代码 commit 一一对应。

#### 步骤三：配置文件传输

使用 **appleboy/scp-action** 将本地最新的 `docker-compose.yml` 安全拷贝到阿里云服务器的 `/opt/starlore` 路径下，确保远程环境配置与本地一致。

#### 步骤四：远程服务更新与清理

通过 **appleboy/ssh-action** 执行远程 SSH 指令，完成一套"组合拳"：

```bash
# 拉取最新镜像
docker-compose pull

# 停止旧容器
docker-compose down

# 启动新容器（后台运行）
docker-compose up -d

# 清理冗余旧镜像
docker image prune -f
```

* * *

## 三、实战中的痛点与优化

### 3.1 问题发现

在流水线稳定运行后，我们发现一个严重性能瓶颈：

> 由于国内阿里云服务器直连海外的 ghcr.io 存在严重的跨境网络丢包与限速，拉取仅 6.7MB 的前端静态资源层镜像时，经常耗时超过 10 分钟！

### 3.2 报错症状

GitHub Actions 工作流频繁报告 **appleboy/ssh-action 连接超时中断**，错误信息为：

```
Run Command Timeout
```

这意味着 SSH 会话在等待 Docker 拉取镜像的过程中超时断开，导致整个部署流程失败。

### 3.3 优化方案

针对这一痛点，我们采用了以下优化策略：

优化手段

方案说明

效果

**镜像加速代理**

在阿里云服务器配置 Docker 镜像加速器，或通过代理中转 ghcr.io 流量

拉取速度提升 5-10 倍

**增加 SSH 超时时间**

在 appleboy/ssh-action 中设置更长的 `timeout` 参数

避免任务被过早中断

**使用阿里云容器镜像服务**

将 GHCR 镜像同步至阿里云 ACR，利用内网高速拉取

从根本上解决跨境网络问题

最终我们采用了**方案一与方案二结合**的方式，将部署耗时从 10+ 分钟压缩至 **2 分钟以内**，极大地提升了部署效率与稳定性。

* * *

## 四、总结与展望

通过本次 DevOps 自动化部署实践，成功实现了：

-   ✅ **一键自动化部署**：推送代码到 main 分支即完成全流程
-   ✅ **容器化交付**：统一的环境保障了开发与生产的一致性
-   ✅ **问题快速定位**：通过日志和超时机制及时发现问题并优化

未来我们计划进一步引入 **健康检查（Health Check）** 与 **滚动更新** 策略，实现零停机部署，为用户提供更加稳定的服务体验。

* * *

> 项目仓库：GitHub - Starlore 技术栈：Vue 3 + Vite | Spring Boot 3 + JWT | Docker + GHCR | GitHub Actions' WHERE id = 60;

-- 62: 面试准备---知识
UPDATE articles SET content = '#### 1\\. RabbitMQ 的核心机制是什么？

RabbitMQ 是基于 AMQP 协议的开源消息中间件，核心采用生产者、Broker、消费者三层架构。

通过交换机将消息路由至队列，实现系统解耦。

它最核心的是全链路消息可靠性保障：生产者端通过 Confirm 和 Return 机制保证投递可靠，Broker 端靠三层持久化防止数据丢失，消费者端通过手动 Ack 确保消费成功。整体用来完成异步解耦、流量削峰的核心作用。

#### 2\\. 怎么理解 Java IO 多路复用？

Java IO 多路复用是 NIO 的核心机制，核心是通过一个 Selector 选择器，用单线程管理多个 Channel 连接，批量监听连接的读写事件，只有事件真正就绪时才触发业务处理，不用为每个连接单独创建线程，大幅减少线程切换和内存开销

#### 3.你怎么理解高并发？

高并发指系统短时间内承载大量用户请求，核心目标是保障系统在高流量下稳定可用、响应及时。 实现高并发是多层级的优化：

-   前端：CDN加速、静态资源缓存、接口合并减少请求数
-   网关：限流、熔断、负载均衡，流量均匀分发
-   服务层：服务无状态化，支持水平扩容；非核心逻辑异步化处理
-   缓存层：Redis多级缓存，承接绝大多数读请求
-   数据库：索引优化、读写分离、分库分表 核心思路：能不查库就不查库，能异步就不同步，能横向扩容就不单机硬扛。

## Java基础高频题

### 1\\. 深拷贝和浅拷贝的区别

-   浅拷贝：仅复制对象的引用地址，新旧对象共享同一块内存，修改其中一个另一个会同步变化，相当于创建快捷方式
-   深拷贝：完整复制对象的全部内容，包括嵌套的引用对象，新旧对象对应完全独立的两块内存，修改互不影响 举例：如果对象包含一个引用类型成员变量，浅拷贝后两个对象的该成员指向同一地址；深拷贝则会把该成员也复制一份。

### 2\\. Java线程的控制方法有哪些？sleep和yield的区别？

**常用线程控制方法**：

-   start()：启动线程，进入就绪状态
-   run()：定义线程执行逻辑，需重写
-   sleep()：线程休眠指定时长，休眠期间不释放锁
-   yield()：让出CPU执行权，让同优先级线程获得执行机会
-   join()：等待该线程执行完毕，再继续当前线程
-   interrupt()：向线程发送中断信号，不强制终止
-   setDaemon()：设置为守护线程

**sleep与yield的区别**：

-   sleep使线程进入阻塞状态，指定时间内不会被调度；yield仅让出CPU，线程仍处于就绪态，可能立即被再次调度
-   sleep会抛出InterruptedException；yield不会
-   sleep与优先级无关；yield仅会让渡给同优先级或更高优先级线程

### 3\\. 什么是多态？举个例子

多态是面向对象三大特性之一，指同一次方法调用，根据对象的实际类型不同，执行不同的行为。实现多态的三个必要条件：继承、方法重写、父类引用指向子类对象。 举例：定义父类`Animal`，包含`eat()`方法；子类`Cat`和`Dog`分别重写`eat()`。当`Animal`类型引用指向`Cat`对象时，调用`eat()`执行猫的进食逻辑；指向`Dog`对象时执行狗的进食逻辑，这就是多态。

### 4\\. Java创建对象的方式有哪些（除了new）？

除了 new 关键字，Java 常用的创建对象方式主要有三种。首先是**反射创建**，通过 `Constructor` 类的 `newInstance` 方法生成对象，支持调用有参构造、私有构造器，也是 Spring 等框架创建 Bean 的核心底层方式；第二种是**克隆（clone）**，对象实现 `Cloneable` 接口并重写 clone 方法后，直接复制内存生成新对象，全程不会调用构造方法；第三种是**反序列化**，实现 `Serializable` 接口的对象，可以从字节流中还原出实例，同样不触发构造方法，多用于对象的网络传输和持久化。除此之外还有底层 `Unsafe` 类直接分配内存的方式，业务开发基本不会用到。

### 5\\. JVM常见的OOM异常有哪些？

除程序计数器外，其余内存区域均可能发生OOM：

1.  堆溢出（Java heap space）：最常见，对象实例超出堆内存容量
2.  栈溢出（StackOverflowError）：方法调用层级过深，如无限递归
3.  方法区/元空间溢出：加载类数量过多，如大量动态生成类
4.  直接内存溢出：NIO直接内存分配超出系统限制

### 6\\. 全局变量与局部变量的区别

-   作用域：全局变量（成员变量）整个类内有效；局部变量仅在方法/代码块内有效
-   存储位置：成员变量存于堆内存的对象中；局部变量存于栈内存
-   默认值：成员变量有默认初始值；局部变量无默认值，不赋值无法使用
-   生命周期：成员变量随对象创建销毁；局部变量随方法调用结束释放

### 7\\. 面向对象与面向过程的区别

-   面向过程以执行流程为核心，将问题拆解为步骤、用函数依次实现，强调“怎么做”，适合简单线性场景
-   面向对象以事物为核心，将问题拆解为对象，对象包含属性与方法，强调“谁来做”，具备封装、继承、多态特性，适合复杂、需扩展的大型项目

### 8\\. Java 和 Python 的优缺点分别是什么？

**Java**

-   优点：强静态类型语言，编译期即可发现类型错误；基于JVM实现跨平台，企业级生态极其成熟；性能优异，是高并发后端、大型分布式系统的主流选择。
-   缺点：语法相对繁琐，代码量更大；启动速度慢，不适合轻量脚本场景。

**Python**

-   优点：语法简洁，开发效率极高，上手成本低；生态极其丰富，在AI应用、数据分析、自动化脚本领域是绝对主流；动态语言灵活度高。
-   缺点：动态类型，类型错误只能在运行时暴露；GIL全局解释器锁导致多线程CPU密集场景性能弱；运行速度相比Java有明显差距。

### 9\\. Java 变量的存储位置？

1.  局部变量：存储在栈内存中，随方法调用创建、方法结束销毁，无默认初始值。
2.  实例变量（成员变量）：存储在堆内存的对象实例中，随对象生灭，有默认初始值。
3.  静态变量（类变量）：存储在方法区/元空间中，属于类本身，类加载时初始化，全局共享。
4.  字符串常量：存储在字符串常量池中，属于方法区的一部分。

### 10\\. 继承有什么作用？

1.  代码复用：子类可以直接复用父类的属性与方法，减少重复代码。
2.  功能扩展：子类可以在父类基础上新增方法，或重写父类方法实现功能扩展。
3.  多态基础：继承是实现多态的前提，父类引用可以指向不同的子类对象。
4.  统一规范：通过父类定义统一的接口标准，所有子类遵循同一套行为规范。

### 11\\. java IO，字符字节

Java IO 是 Java 提供的数据输入输出机制，主要用于文件操作、网络通信等场景。Java IO 主要分为字节流和字符流，字节流以 byte 为单位处理数据，主要用于图片、视频等二进制文件，对应 InputStream 和 OutputStream；字符流以字符为单位处理文本数据，对应 Reader 和 Writer，内部会进行字符编码转换，更适合处理文本文件。

### 12\\. java注解机制原理

Java 注解本质上是一种元数据，它本身不会主动执行逻辑。注解通过 @interface 定义，编译后会根据 @Retention 的保留策略保存到源码、Class 文件或运行时。像 RUNTIME 类型的注解，框架可以在程序运行时通过反射获取类、方法或字段上的注解信息，再根据注解内容执行对应处理。

## 四、计算机网络高频题

### 1\\. 跨域问题是什么？怎么解决？

跨域由浏览器同源策略导致，协议、域名、端口三者任一不同即触发跨域，浏览器会拦截跨域响应，目的是防止恶意网站窃取数据。 **常见解决方案**：

1.  后端CORS配置：在响应头添加Access-Control-Allow-Origin等字段，允许指定域名跨域，是生产环境主流方案
2.  前端代理：开发环境通过Vite/Webpack代理转发请求，绕过浏览器同源限制
3.  Nginx反向代理：将前后端请求代理至同一域名，从根源消除跨域
4.  JSONP：仅支持GET请求，现已较少使用

面试官您好。跨域本质上是由浏览器的***同源策略（Same-Origin Policy）\\*引起的安全限制。当我们在前端向后端发送请求时，如果\\*协议、域名、端口***这三者中有任何一个不同，浏览器就会拦截接收到的响应数据（请求其实发出去了，但是浏览器拒绝把响应交回给前端）。

Vite 的 `server.proxy` 配置了本地代理。它的核心原理是：**跨域限制只存在于浏览器，服务器与服务器之间通信是没有同源策略限制的。**Vite Proxy 是开发环境的代理服务，通过本地 Node 服务器转发接口请求， 避开浏览器同源限制，用来解决开发时的跨域问题。

我配置了 `**Nginx 反向代理（proxy_pass）**`。我把前端打包后的静态资源，和 Node.js 后端提供的 API 接口，通过 Nginx 代理到了同一个域名和同一个端口下。这样一来，从物理架构和浏览器的视角来看，前端和后端的通信完全属于同源，从根本上抹除了跨域问题。”

### 2\\. HTTP状态码301、302、304分别是什么？

均为3xx重定向类状态码：

-   301 永久重定向：资源已永久迁移至新地址，浏览器会永久缓存，后续直接访问新地址
-   302 临时重定向：资源临时位于其他地址，浏览器下次仍请求原地址
-   304 未修改：协商缓存标识，浏览器携带缓存标识请求，服务器判定资源未变更，返回304，浏览器直接使用本地缓存，不返回资源体

### 3\\. 用户访问一个网页的完整流程

1.  输入URL，浏览器解析协议、域名、路径
2.  DNS解析：依次查询本地缓存、hosts、本地DNS服务器、根DNS，最终获取服务器IP
3.  建立连接：TCP三次握手，HTTPS额外完成TLS握手
4.  发送HTTP请求：浏览器组装请求头、请求体并发送
5.  服务器处理：服务端执行业务逻辑，返回HTTP响应
6.  浏览器解析渲染：解析HTML生成DOM树，解析CSS生成渲染树，完成布局与绘制
7.  执行JS脚本：处理页面交互逻辑
8.  页面加载完成，触发load事件

### 4\\. HTTP与TCP、UDP的关系

HTTP是应用层协议：

-   HTTP/1.1、HTTP/2 底层基于TCP传输，面向连接、可靠传输
-   HTTP/3 为解决TCP队头阻塞问题，基于UDP的QUIC协议实现，在UDP之上封装了可靠传输、拥塞控制等能力，性能更优

### 5\\. Web开发常见的安全问题

1.  XSS跨站脚本攻击：注入恶意脚本，解决方案：输入过滤、输出转义
2.  CSRF跨站请求伪造：冒充用户发起请求，解决方案：Token校验、Referer校验
3.  SQL注入：注入恶意SQL语句，解决方案：预编译语句、参数化查询，禁止SQL拼接
4.  敏感数据泄露：明文存储/传输敏感信息，解决方案：加密存储、HTTPS传输
5.  文件上传漏洞：上传恶意文件，解决方案：校验文件类型、重命名、存储于安全位置
6.  越权访问：未做权限校验，解决方案：接口全量权限校验，遵循最小权限原则

### 6\\. 为什么虚拟地址空间切换比较耗时？

1.  上下文切换：需保存当前进程寄存器、程序计数器等状态，加载新进程上下文，数据拷贝开销大
2.  TLB刷新：TLB是地址翻译缓存，进程切换后全部失效，后续地址转换延迟大幅上升
3.  页表切换：每个进程有独立页表，切换时更换页表基址，需访问内存读取新页表
4.  CPU缓存失效：切换后CPU缓存的旧进程数据全部失效，需重新加载，缓存命中率骤降

### 7\\. TCP/IP 四层模型分别是什么？

从上到下依次为：

1.  应用层：直接对接应用程序，对应协议：HTTP、HTTPS、DNS、FTP。
2.  传输层：负责端到端的数据传输与可靠性保障，对应协议：TCP、UDP。
3.  网络层：负责主机间寻址、路由选择，对应协议：IP、ICMP、ARP。
4.  网络接口层：负责物理介质上的比特传输与帧转发，对应以太网、MAC地址等。

### 8\\. 同一个局域网内的两台主机怎么通信？

1.  发送方先通过ARP地址解析协议，根据目标IP地址查询到目标主机的MAC地址。
2.  将数据封装为以太网数据帧，填入源MAC与目标MAC地址。
3.  数据帧发送到交换机，交换机根据MAC地址表，将数据帧转发到目标主机对应的端口。
4.  目标主机收到数据帧，校验MAC地址匹配后接收数据，向上层协议解析处理。 如果是跨网段通信，则需要通过网关路由器进行三层转发。

* * *

## 五、MySQL高频题

### 1\\. char和varchar的区别

维度

char

varchar

长度特性

定长，存不满用空格补齐

变长，存多少占多少，额外存储长度信息

存储效率

短字符串性能高，无额外开销

长字符串更节省空间

长度上限

最多255字符

最多65535字节

适用场景

长度固定的内容，如手机号、身份证号

长度不定的内容，如用户名、标题

### 2\\. limit分页有什么问题？怎么优化？

**核心问题**：

-   深分页性能差：offset越大，MySQL需扫描并跳过的行数越多，查询越慢
-   内存浪费：需将offset前的所有数据加载至内存再丢弃，资源消耗高
-   数据不一致：分页期间数据增删，可能出现重复或遗漏

**优化方案**：

-   主键游标法：记录上一页最后一个主键ID，通过where id > 最后ID limit n查询，避免大offset
-   延迟关联：先查询主键ID，再通过ID回表查完整数据，减少扫描量
-   业务限制分页深度，不允许查询过深的页码

### 3\\. MySQL事务四大特性？隔离性怎么保证？

四大特性ACID：

-   原子性：事务要么全成功要么全失败，由undo log回滚日志保证
-   一致性：事务前后数据完整性与约束不被破坏，是事务的最终目标
-   隔离性：多事务间互不干扰，由MVCC多版本并发控制 + 锁机制保证
-   持久性：事务提交后数据永久生效，由redo log重做日志保证

隔离性通过事务隔离级别实现，InnoDB默认可重复读级别：通过MVCC实现读写不冲突，配合行锁、间隙锁解决幻读问题，保障事务间的隔离效果。

### 4\\. 一页30条，查询第5页的SQL怎么写？

第5页需跳过前4页共120条数据：

```
SELECT * FROM 表名 LIMIT 120, 30;
```

### 5\\. MySQL 读取数据的方式有哪些？

1.  基础SELECT查询：指定字段、条件、排序、分页，是最常用方式。
2.  JOIN多表连接：通过内连接、左连接、右连接等关联多张表，读取关联数据。
3.  子查询：将一个查询结果作为另一个查询的条件或数据源，嵌套执行。
4.  视图：基于查询结果的虚拟表，封装复杂查询逻辑，简化上层调用。
5.  存储过程/函数：预编译的SQL集合，调用后返回查询结果。

* * *

### 5\\. mysql索引

MySQL索引是一种帮助数据库快速定位数据的数据结构，类似书籍目录。InnoDB中主要使用B+Tree作为索引结构，因为B+Tree层级低、磁盘IO次数少，并且叶子节点通过链表连接，适合范围查询。常见索引包括主键索引、普通索引、唯一索引和联合索引。使用联合索引时需要注意最左匹配原则。索引虽然可以提升查询性能，但是会增加存储空间，同时降低插入和更新效率，因为数据变化时需要同步维护索引。另外在实际开发中需要注意索引失效问题，例如函数操作、隐式类型转换、左模糊查询等。

## 六、Redis高频题

### 1\\. Redis怎么解决并发竞争问题？

1.  原子命令：Redis单线程执行，单个命令天然原子，如INCR、SETNX，适用于简单场景
2.  事务：通过MULTI+EXEC打包多条命令，保证原子执行
3.  乐观锁：WATCH命令监听key，事务执行前key被修改则事务取消
4.  分布式锁：SET key value NX EX实现互斥锁，同一时间仅一个客户端持有，是分布式场景主流方案
5.  队列：用List做任务队列，消费者串行消费，从根源避免竞争

### 2\\. Redis缓存三大问题：穿透、击穿、雪崩

-   缓存穿透：查询不存在的数据，请求直接穿透到数据库。解决方案：缓存空值、布隆过滤器前置过滤
-   缓存击穿：单个热点key过期瞬间，大量并发请求直击数据库。解决方案：热点key永不过期、加互斥锁重建缓存
-   缓存雪崩：大量key同时过期，或Redis宕机，全量请求打向数据库。解决方案：过期时间加随机值打散、搭建Redis集群、服务熔断降级

### 3\\. 怎么查询Redis中占用空间大的key？

Redis 4.0以上版本，使用`SCAN` + `MEMORY USAGE`方案：

1.  用SCAN 0 COUNT 1000分批遍历所有key，避免KEYS命令阻塞
2.  对每个key执行MEMORY USAGE key，获取其占用字节数
3.  收集结果排序，定位大key 低版本可通过redis-rdb-tools分析RDB文件实现。

### 4\\. Redis内存溢出怎么处理？

1.  扩容：调大maxmemory配置，提升内存上限
2.  优化内存：选用更节省空间的数据结构、数据压缩、清理过期键
3.  淘汰策略：配置内存淘汰策略，如LRU最近最少使用、LFU最不经常使用
4.  数据分片：拆分至多个Redis实例，分散单实例压力
5.  持久化卸载：冷数据落盘持久化，释放内存空间

### 5\\. Redis 有哪些数据结构？

-   基础5种核心结构：String（字符串，用于计数、缓存、分布式锁）、List（双向链表，用于队列）、Hash（键值对集合，用于存储对象）、Set（无序去重集合，用于去重、交集计算）、ZSet（有序去重集合，用于排行榜、延时队列）。
-   高级结构：HyperLogLog（基数统计）、Geo（地理位置计算）、Bitmap（位图统计）、Stream（原生消息队列）。

* * *

## 七、前端高频题（匹配你的技术栈）

### 1\\. Vue3 Composition API与Options API的区别

-   Options API是选项式写法，代码按data、methods、computed等分选项组织，逻辑分散在各处，组件复杂后维护成本高
-   Composition API是组合式写法，在setup函数中组织代码，可将相关逻辑抽离为组合函数，逻辑更内聚、复用性更强，对TypeScript支持更友好 Vue3官方推荐Composition API，更适合中大型项目。

### 2\\. 虚拟滚动的实现原理

核心思想：仅渲染视口内可见的元素，大幅减少DOM节点数。

1.  根据视口高度与单行高度，计算可见元素数量
2.  监听滚动事件，通过滚动偏移量计算当前可见数据的起始索引
3.  仅渲染可见范围内的数据，同时用占位元素撑起列表总高度，保证滚动条正常
4.  滚动时动态替换渲染数据，DOM节点数始终维持在低位，万级数据也能流畅滚动

### 3\\. 如何防止接口重复提交？

**前端层面**：请求发出后禁用提交按钮、增加loading状态，防止用户重复点击 **后端层面**：

1.  幂等性设计：接口本身实现幂等，如通过唯一业务单号去重
2.  Token机制：请求前申请唯一Token，提交后立即删除，重复请求直接拒绝
3.  Redis分布式锁：同一用户+相同参数加锁，处理完成后释放
4.  请求去重表：数据库存储请求ID，加唯一索引实现去重

* * *

## 八、手撕算法题（高频必考题）

### 1\\. 1~n的全排列（LeetCode 46）

回溯法实现：

```
     const path = [];
     const used = new Array(nums.length).fill(false);
     const dfs = () => {
         // 路径长度等于数组长度，收集结果
         if (path.length === nums.length) {
             res.push([...path]);
             return;
         }
         // 遍历所有元素，选未使用的
         for (let i = 0; i < nums.length; i++) {
             if (used[i]) continue;
            used[i] = true;
             path.push(nums[i]);
             dfs();
             // 回溯：撤销选择
             path.pop();
             used[i] = false;
         }
     };
     dfs();
     return res;
```

### 2\\. 合并两个有序数组

双指针从后往前填充：

```
class Solution {
public void merge(int[] nums1, int m, int[] nums2, int n) {
   int i = m - 1, j = n - 1, k = m + n - 1;
   while (i >= 0 && j >= 0) {
   nums1[k--] = nums1[i] > nums2[j] ? nums1[i--] : nums2[j--];
    }
    while (j >= 0)
    nums1[k--] = nums2[j--];
    }
    }
```

### 3\\. 数组向右移动k位

三次翻转法，空间复杂度O(1)：

```
class Solution {
public void rotate(int[] nums, int k) {
k %= nums.length;
reverse(nums, 0, nums.length - 1);
reverse(nums, 0, k - 1);
reverse(nums, k, nums.length - 1);    }​
 private void reverse(int[] nums, int left, int right) {
       while (left < right) {
       int temp = nums[left];
       nums[left] = nums[right];
        nums[right] = temp;            left++;
           right--;
             }
             }}
```

### 1\\. 最近遇到的比较大的bug是什么？怎么解决的？

最近印象最深的是AI知识库项目里，多Agent并行执行子任务时出现上下文错乱，多个子任务的大模型返回结果互相串扰。 一开始以为是线程安全问题，排查后发现是LangChain的Prompt模板对象被多个线程共享修改了。解决方案是给每个子任务创建独立的Prompt实例与Chain对象，完全不共享可变对象，问题就彻底解决了。 这件事也让我意识到，并发场景下哪怕看似无状态的对象，也要警惕共享可变状态带来的隐患。

### 2\\. 空闲时间一般做什么？

空闲时间大部分还是围绕技术，比如持续迭代我的个人知识库项目，也会看技术博客和开源项目学习新技术，比如最近一直在研究大模型Agent和前端可视化相关的内容。偶尔会打打游戏放松一下。

### 3\\. 逛过哪些技术类论坛？

平时用的最多的是GitHub，看优秀开源项目的源码和设计；国内会看掘金、CSDN查问题、读技术文章，也刷牛客看面经、刷算法题；国外会用Stack Overflow查问题，偶尔看Medium的技术文章。

### 4\\. 怎么看待互联网加班？

我认为项目赶进度、临上线节点的时候加班是正常的，我也完全可以接受。但我更认可高效的工作方式，尽量在工作时间内高质量完成任务，而不是无意义的低效加班。如果是为了项目目标、为了解决关键问题加班，我觉得是有价值的，我也愿意为结果付出时间。

### 5\\. 关注了哪些技术热点？

我比较关注大模型应用开发方向，比如Agent编排、RAG优化、MCP协议这些新方向，自己也在做相关的项目实践。前端方向会关注新的框架特性和性能优化方案，后端也会持续关注微服务与云原生相关的技术演进。

### 6\\. 薪资期望是多少？

我更看重这个岗位的成长空间和能学到的东西，薪资按照公司校招的统一标准就可以，我相信公司会给出合理的待遇。

### 7\\. 淘宝个性化推荐商品，用到了哪些技术点？

整体分为四层技术体系：

1.  数据采集层：通过用户埋点采集浏览、点击、收藏、加购、购买等全链路行为，用Flink做实时计算、Hive做离线数仓存储，构建用户画像与商品画像。
2.  算法召回层：多路召回策略，包括协同过滤（用户/物品维度）、内容标签召回、热门召回、深度学习召回；再通过排序模型（如Wide&Deep、DIN）做精排。
3.  工程架构层：Redis多级缓存缓存用户画像与热门商品，推荐结果预计算；微服务化部署，通过降级、限流保障高并发下的稳定性。
4.  效果迭代层：配套A/B测试平台，通过点击率、转化率、停留时长等指标持续迭代算法策略。' WHERE id = 62;

COMMIT;

SELECT id, title, LEFT(content, 120) AS content_preview FROM articles WHERE id IN (18, 19, 22, 27, 29, 30, 32, 33, 34, 35, 54, 56, 57, 58, 60, 62) ORDER BY id;
SET FOREIGN_KEY_CHECKS = 1;

-- 如需回滚：
-- UPDATE articles a JOIN `articles_content_backup_20260722095827` b ON b.id = a.id SET a.content = b.content, a.updatedAt = b.updatedAt;
