package main

import (
	"context"
	"github.com/chromedp/chromedp"
	"github.com/kataras/iris/v12"
	"log"
	"time"

	//"time"
)

func main()  {
	app :=iris.New()

	app.Post("/getReportDatas", func(ctx iris.Context) {
		options := []chromedp.ExecAllocatorOption{
			chromedp.Flag("headless", true), // debug使
			chromedp.Flag("disable-gpu",true),
			chromedp.Flag("no-sandbox",true),
			chromedp.UserAgent(`Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36`),
		}
		//初始化参数，先传一个空的数据
		options = append(chromedp.DefaultExecAllocatorOptions[:], options...)

		c, _ := chromedp.NewExecAllocator(context.Background(), options...)
		// create context
		chromeCtx, cancel := chromedp.NewContext(c, chromedp.WithLogf(log.Printf))
		// 执行一个空task, 用提前创建Chrome实例
		err1:=chromedp.Run(chromeCtx, make([]chromedp.Action, 0, 1)...)
		if(err1!=nil){
			log.Println(err1)
		}
		//创建一个上下文，超时时间为40s
		defer cancel()
		timeoutCtx, cancel := context.WithTimeout(chromeCtx, 25*time.Second)
		defer cancel()
		urls :=Urls{}
		// run task list
		ctx.ReadJSON(&urls)
		var res  string
		var res2  string
		var res3  string
		var res4  string
		err := chromedp.Run(timeoutCtx,
			chromedp.Navigate(urls.ViewOnly),
			//chromedp.WaitVisible("#sc2708"),
			//chromedp.WaitVisible("#sc2925"),
			chromedp.Sleep(2 * time.Second),
			chromedp.Evaluate(`document.getElementsByClassName("sc-view sc-label-view infobar-value")[0].innerText`, &res),
			chromedp.Evaluate(`document.getElementsByClassName("percent")[0].innerText`,&res2),
			chromedp.Evaluate(`document.getElementsByClassName("percent")[1].innerText`,&res3),
			chromedp.Evaluate(`document.getElementsByClassName("percent")[2].innerText`,&res4),
		)
		if err != nil {
			log.Println(err)
			ctx.WriteString("false")//爬取错误
		}
		//chromedp.Cancel(timeoutCtx)
		//chromedp.Cancel(chromeCtx)
		ctx.WriteString(res+";"+res2+";"+res3+";"+res4)

	})
	app.Listen(":80")
}

type Urls struct {
	ViewOnly    string `json:"view_only_url"`
	Url  string `json:"report_url"`
}



