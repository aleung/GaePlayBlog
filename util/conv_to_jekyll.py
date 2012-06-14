#!/usr/bin/env python
# -*- coding: utf-8 -*-


import html2text # https://github.com/aleung/html2text
import json
import sys
import codecs
import re
import string
from datetime import datetime
from urllib import urlopen, urlencode
from configuration import config

tag_translate = {
    "电脑使用":"Software",
    "软件开发":"SoftwareDev",
    "软件技术":"SoftwareDev",
    "胡思乱想":"Idea",
    "生活":"Life",
    "读书":"Reading",
    "时间管理":"TimeManagement",
    "图像处理":"ImageProcessing",
    "杂谈":"Misc",
    "GPS_&amp;_GIS":"GPS_GIS",
    "小说":"Novel",
}

def youdao_translate(source):
    if not re.match(r'[^\x00-\x7f]', source):
        return source
    params = urlencode({'keyfrom':config['keyfrom'], 'key':config['key'], 'type':'data', 'doctype':'json', 'version':'1.1', 'q':source.encode('utf-8')})
    api = 'http://fanyi.youdao.com/openapi.do?%s' % params
    resultJson = urlopen(api).read().decode('utf-8')
    result = json.loads(resultJson)
    if result['errorCode'] == 0:
        return result['translation'][0]
    return source

def formalize_title(title):
    eng_title = youdao_translate(title)
    return re.sub(r'[^a-zA-Z0-9]+', '-', string.strip(eng_title))


def convert_to_markdown(json_line):
    article = json.loads(json_line)
    print 'Converting ID:%d' % article['id']
    init_date = datetime.fromtimestamp(article['initialDate']/1000)
    update_date = datetime.fromtimestamp(article['date']/1000)
    filename = init_date.strftime('%Y-%m-%d-') + formalize_title(article['title']) + '.markdown'
    with codecs.open(filename, 'w', 'utf-8') as output:
        output.write('---\n')
        output.write('layout: post\n')
        output.write('title: "%s"\n' % article['title'])
        output.write('comments: true\n')
        output.write('date: %s\n' % init_date.strftime("%Y-%m-%d %H:%M"))
        if article['date'] - article['initialDate'] > 3600 * 1000:
            output.write('updated: %s\n' % update_date.strftime("%Y-%m-%d %H:%M"))
        if article['tags']:
            output.write('tags:\n')
            for tag in article['tags']:
                encoded_tag = tag.encode('utf-8')
                print encoded_tag
                if encoded_tag in tag_translate:
                    tag_name = tag_translate[encoded_tag]
                else:
                    tag_name = tag
                print tag_name
                output.write('- %s\n' % tag_name)
        output.write('---\n')
        output.write(html2text.html2text(article['content']))

def main():
    f = codecs.open(sys.argv[1], 'U', 'utf-8')
    while True:
        line = f.readline()
        if not line: break
        convert_to_markdown(line)
    f.close()


if __name__ == "__main__":
    main()