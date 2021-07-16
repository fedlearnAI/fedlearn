纵向多分类数据集 - 叶子分类 B

需要设置：
(1)objective= multi:softmax 或 multi-softprob
(2)设置 numClass (类别数量)
数据集中类别 label 有 3 个, numClass 可以设为大于等于 3 的值, 多出的数量作为 missing label 处理

数据源:
https://archive.ics.uci.edu/ml/datasets/Leaf

数据集信息:
(1)样本数量
训练集 11 条
测试集 2 条
(2)Attribute Information:
1. uid (样本 id 已对齐)
2. Specimen Number
3. Eccentricity
4. Aspect Ratio
5. Elongation
6. Solidity
7. Stochastic Convexity
8. Isoperimetric Factor
9. Maximal Indentation Depth
10. Lobedness
11. Average Intensity
12. Average Contrast
13. Smoothness
14. Third moment
15. Uniformity
16. Entropy


