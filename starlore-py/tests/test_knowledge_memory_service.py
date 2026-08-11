"""知识记忆纯函数测试，不访问数据库或外部 AI。"""

import unittest

from app.exceptions import BadRequestException
from app.services.knowledge_memory_service import _decode_model_json, _source_hash


class KnowledgeMemoryServiceTest(unittest.TestCase):
    def test_decodes_json_wrapped_by_model_text(self):
        cards = _decode_model_json(
            '结果如下：\n{"cards":[{"question":"什么是 JVM？","startLine":2,"endLine":5}]}'
        )
        self.assertEqual(cards[0]["startLine"], 2)

    def test_rejects_non_json_model_response(self):
        with self.assertRaises(BadRequestException):
            _decode_model_json("没有可用结果")

    def test_source_hash_changes_with_markdown(self):
        self.assertNotEqual(_source_hash("# A"), _source_hash("# B"))


if __name__ == "__main__":
    unittest.main()
