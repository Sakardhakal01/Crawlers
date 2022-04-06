open System
open System.Collections.Concurrent
open System.Collections.Generic
open System.IO
open System.Net
open System.Text.RegularExpressions
open FSharp.Data
open Microsoft.FSharp.Control.CommonExtensions   

let agents = 10

type Message =
        | CrawlRequest of string * int 
        | Done

let extractLinks html =
        let pattern1 = "(?i)href\\s*=\\s*(\"|\')/?((?!#.*|/\B|mailto:|location\.|javascript:)[^\"\']+)(\"|\')"
        let pattern2 = "(?i)^https?"

        let links =
            [
                for x in Regex(pattern1).Matches(html) do
                    yield x.Groups.[2].Value
            ]
            |> List.filter (fun x -> Regex(pattern2).IsMatch(x))
        links     
 

let saveAsFile (html: string) (url: string) =       
        let formattedUrl = url.Replace("http://", "").Replace("https://", "").Replace("www", "").Replace(".", "_").Replace("/", "_").Replace("?", "_")
        let path = "./output/" + formattedUrl + ".html"
        File.WriteAllText(path, html)

let q = ConcurrentQueue<Message>()
let visited = HashSet<string>()


let collectLinks incomingRequest = async {
        match incomingRequest with
        | CrawlRequest(url, depth) ->
            try
                let req = WebRequest.Create(Uri(url)) 
                use! resp = req.AsyncGetResponse()
                use stream = resp.GetResponseStream()
                let content = resp.ContentType
                let isHtml = Regex("html").IsMatch(content)
                if (isHtml) then                   
                    use reader = new StreamReader(stream)
                    let html = reader.ReadToEnd()
                    saveAsFile html url                          
                    let links = extractLinks html
                    for link in links do
                        q.Enqueue(CrawlRequest(link, depth-1))    
            with
            | _ ->  printfn "Exception occured while fetching: %s , depth: %d" url depth                       
        | _ -> ()
    }

let agent id = async {
        while true do
            let request = q.TryDequeue()
            match request with
            | true, crawlrequest ->
                printfn "Agent %d request: %O" id  crawlrequest
                match crawlrequest with
                | CrawlRequest(url, depth) ->
                    if not(visited.Contains url) && depth >= 0 then
                        let set' = visited.Add url
                        do! collectLinks crawlrequest 
                    else
                        printfn "Crawl request: %O failed" crawlrequest  
                | _ -> ()
            | _ -> printf "" // -> ()
    }

let createWorkers() = 
    [1..10] |> Seq.map agent |> Async.Parallel |> Async.RunSynchronously

let args = System.Environment.GetCommandLineArgs()
for arg in args do
        printfn "%s" arg
let inputFile = args.[3]
let sites = File.ReadLines(inputFile)
let depth = args.[4] |> int

for x in sites do
    q.Enqueue(CrawlRequest(x, depth))
let res = createWorkers()
