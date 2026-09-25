# 软考资料导入

`import_soft_exam_materials.py` 通过管理端 API 导入目录下所有 PDF。它把每页可提取文本和原始页面图片保存到内容库，再按每 12 页生成一节小程序可读讲义。脚本会核对页码、文本和图片对象键，并通过文件 SHA-256 跳过重复资料或继续中断的批次。ZIP 文件不单独导入；若 ZIP 内容已解压为 PDF，请使用解压后的目录。

现有 PDF 题目解析器不能可靠识别所有原卷排版。本脚本不发布题目或知识点候选，也不把模拟题标为历年真题。题目和知识点仍可在管理端导入中心单独审核。

在 txy211 上运行（`--env-file` 只在服务器本地读取，凭据不写入报告）：

```bash
python3 /opt/ruankao/source/scripts/import_soft_exam_materials.py \
  --source /opt/ruankao/import-source \
  --exam-id 1 \
  --env-file /opt/ruankao/source/deploy/txy211/.env \
  --report /opt/ruankao/import-report.json
```

上面的命令只进行解析、入库和页完整性核对。检查报告和管理端导入预览后，加入 `--publish` 再运行同一命令，即可确认并发布原始资料讲义。脚本只自动解决“未识别出结构”这类由逐页讲义取代的问题；其他全局错误会阻止发布。已发布批次再次运行会跳过。报告含文件、哈希、批次、页数、讲义数和课程 ID，不含密码或登录令牌。

运行测试：

```bash
python3 -m unittest discover -s scripts/tests -p 'test_import_soft_exam_materials.py' -v
```
