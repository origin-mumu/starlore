"""RAGAS RAG 质量评估。

提供 RAG 系统质量评估能力，评估指标：
- Faithfulness（忠实度）：回答是否基于检索到的文档
- Answer Relevancy（回答相关性）：回答是否与问题相关
- Context Precision（上下文精确度）：检索的文档是否精确
- Context Recall（上下文召回率）：是否检索到所有相关文档
"""

import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class RAGASEvaluation:
    """RAGAS 评估结果。"""
    faithfulness: float = 0.0
    answer_relevancy: float = 0.0
    context_precision: float = 0.0
    context_recall: float = 0.0
    overall_score: float = 0.0


async def evaluate_rag(
    question: str,
    answer: str,
    contexts: list[str],
    ground_truth: str | None = None,
) -> RAGASEvaluation:
    """评估 RAG 质量。

    Args:
        question: 用户问题
        answer: LLM 生成的回答
        contexts: 检索到的上下文文档列表
        ground_truth: 标准答案（可选，用于计算 recall）

    Returns:
        RAGASEvaluation 评估结果
    """
    try:
        from ragas import evaluate
        from ragas.metrics import (
            faithfulness,
            answer_relevancy,
            context_precision,
            context_recall,
        )
        from datasets import Dataset

        # 构建评估数据集
        data = {
            "question": [question],
            "answer": [answer],
            "contexts": [contexts],
        }
        if ground_truth:
            data["ground_truth"] = [ground_truth]

        dataset = Dataset.from_dict(data)

        # 选择评估指标
        metrics = [faithfulness, answer_relevancy, context_precision]
        if ground_truth:
            metrics.append(context_recall)

        # 运行评估
        result = evaluate(dataset, metrics=metrics)

        # 提取结果
        scores = result.scores[0] if result.scores else {}
        evaluation = RAGASEvaluation(
            faithfulness=scores.get("faithfulness", 0.0),
            answer_relevancy=scores.get("answer_relevancy", 0.0),
            context_precision=scores.get("context_precision", 0.0),
            context_recall=scores.get("context_recall", 0.0) if ground_truth else 0.0,
        )
        evaluation.overall_score = (
            evaluation.faithfulness * 0.4
            + evaluation.answer_relevancy * 0.3
            + evaluation.context_precision * 0.3
        )

        logger.info(
            "RAGAS 评估完成: faithfulness=%.2f, relevancy=%.2f, precision=%.2f, overall=%.2f",
            evaluation.faithfulness,
            evaluation.answer_relevancy,
            evaluation.context_precision,
            evaluation.overall_score,
        )
        return evaluation

    except ImportError:
        logger.warning("RAGAS 未安装，跳过评估")
        return RAGASEvaluation()
    except Exception as e:
        logger.error("RAGAS 评估失败: %s", e)
        return RAGASEvaluation()


async def evaluate_batch(
    questions: list[str],
    answers: list[str],
    contexts_list: list[list[str]],
    ground_truths: list[str] | None = None,
) -> list[RAGASEvaluation]:
    """批量评估 RAG 质量。"""
    results = []
    for i, (q, a, c) in enumerate(zip(questions, answers, contexts_list)):
        gt = ground_truths[i] if ground_truths else None
        result = await evaluate_rag(q, a, c, gt)
        results.append(result)
    return results
