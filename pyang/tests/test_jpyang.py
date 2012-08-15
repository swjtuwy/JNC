"""
Created on 16 jul 2012

@author: emil@tail-f.com

Contains class Test, for function tests. PyUnit is needed to run these tests.

To run, stand in project dir and enter:
$ python -m unittest discover -v
"""
import unittest

from pyang.plugins import jpyang  # @UnresolvedImport
from pyang.tests import util  # @UnresolvedImport
from pyang.statements import Statement


class Test(unittest.TestCase):
    """Contains all JPyang function tests"""

    def setUp(self):
        """Runs before each test"""
        # Initialize context with directory 'gen'
        util.init_context(self)
        util.test_default_context(self)

        # Construct a statement tree: c, c/l, c/leaf, c/l/key and c/l/k
        self.c = Statement(None, None, None, 'container', arg='c')
        self.l = Statement(self.c, self.c, None, 'list', arg='l')
        self.leaf = Statement(self.c, self.c, None, 'leaf', arg='leaf')
        self.key = Statement(self.l, self.l, None, 'key', arg='k')
        self.k = Statement(self.l, self.l, None, 'leaf', arg='k')

    def tearDown(self):
        """Runs after each test"""
        pass

    def testCapitalize_first(self):
        """Simple cases of the capitalize_first function"""
        res = jpyang.capitalize_first('A')
        assert res == 'A', 'was: ' + res
        res = jpyang.capitalize_first('Ab')
        assert res == 'Ab', 'was: ' + res
        res = jpyang.capitalize_first('AB')
        assert res == 'AB', 'was: ' + res
        res = jpyang.capitalize_first('aB')
        assert res == 'AB', 'was: ' + res
        res = jpyang.capitalize_first('ab')
        assert res == 'Ab', 'was: ' + res
        res = jpyang.capitalize_first('teSt')
        assert res == 'TeSt', 'was: ' + res

    def testCamelize(self):
        """Special, "unlikely" cases of the camelize function

        Does not test for removal of any characters other than - and .

        """
        res = jpyang.camelize('teSt')
        assert res == 'teSt', 'was: ' + res
        res = jpyang.camelize('a.weird-stringThis')
        assert res == 'aWeirdStringThis', 'was: ' + res
        res = jpyang.camelize('...')
        assert res == '..', 'was: ' + res
        res = jpyang.camelize('.-.')
        assert res == '-.', 'was: ' + res
        res = jpyang.camelize('.-')
        assert res == '-', 'was: ' + res
        res = jpyang.camelize('.-a')
        assert res == '-a', 'was: ' + res
        res = jpyang.camelize('a-.')
        assert res == 'a.', 'was: ' + res
        res = jpyang.camelize('a-')
        assert res == 'a-', 'was: ' + res
        res = jpyang.camelize('-a')
        assert res == 'A', 'was: ' + res

    def testGet_package(self):
        """Correct package is retrieved for all nodes in the statement tree

        Perform tests on all nodes in the tree. The top level statement and its
        immediate children should have the base package.

        """
        directory = self.ctx.opts.directory

        res = jpyang.get_package(self.c, self.ctx)
        assert res == directory, 'was: ' + res
        res = jpyang.get_package(self.leaf, self.ctx)
        assert res == directory, 'was: ' + res
        res = jpyang.get_package(self.l, self.ctx)
        assert res == directory, 'was: ' + res
        res = jpyang.get_package(self.key, self.ctx)
        assert res == directory + '.l', 'was: ' + res
        res = jpyang.get_package(self.k, self.ctx)
        assert res == directory + '.l', 'was: ' + res

    def testPairwise(self):
        """The iterator includes the next item also"""
        l = [1, 2, 3]

        # Test that the next() method returns correct values
        res = jpyang.pairwise(l)
        for i in range(len(l)):
            if i != len(l) - 1:
                assert res.next() == (l[i], l[i + 1])
            else:
                assert res.next() == (l[i], None)

        # Test that next_item contains correct value during iteration
        res = jpyang.pairwise(l)
        i = 0
        prev = l[0]
        for item, next_item in res:
            assert item != None
            assert item == prev
            prev = next_item
            i += 1
        assert i == len(l), '#iterations (should be ' + len(l) + '): ' + str(i)

    def testFlatten(self):
        """Able to flatten list structures"""
        # Empty list
        res = jpyang.flatten([])
        assert res == [], 'was: ' + res

        # Nested structure of empty lists
        res = jpyang.flatten([[[[], []]], [[]]])
        assert res == [], 'was: ' + res

        # Simple case with integers
        res = jpyang.flatten([[1, 2], 3])
        assert res == [1, 2, 3], 'was: ' + res

        # Simple case with strings
        res = jpyang.flatten([['12', '34'], ['56', ['7']]])
        assert res == ['12', '34', '56', '7'], 'was: ' + res

        # Dictionary
        res = jpyang.flatten({'a': 1, 'b': 2})
        assert res == [1, 2], 'was: ' + res

        # Nested dictionary, with list
        res = jpyang.flatten({'a': {'a': 1, 'b': 2}, 'b': [3, [4]]})
        assert res == [1, 2, 3, 4], 'was: ' + res

    def testGet_types(self):
        """Type conversions for string, int32, etc."""
        # String
        stmt = Statement(None, None, None, 'type', arg='string')
        confm, primitive = jpyang.get_types(stmt, self.ctx)
        assert confm == 'com.tailf.jnc.YangString', 'was: ' + confm
        assert primitive == 'String'

        # int32 - signed, so xs.Int and int is used
        stmt = Statement(None, None, None, 'type', arg='int32')
        confm, primitive = jpyang.get_types(stmt, self.ctx)
        assert confm == 'com.tailf.jnc.YangInt32', 'was: ' + confm
        assert primitive == 'int', 'was: ' + primitive

        # uint32 - unsigned, so xs.UnsignedLong and long are used (same as 64)
        stmt = Statement(None, None, None, 'type', arg='uint32')
        confm, primitive = jpyang.get_types(stmt, self.ctx)
        assert confm == 'com.tailf.jnc.YangUInt32', 'was: ' + confm
        assert primitive == 'long', 'was: ' + primitive

        # TODO: Test typedefs, other non-type stmts, None and remaining types

    def testGet_base_type(self):
        """Correct result from get_base_type for different statements"""
        # Statement not containing a type at all should return None
        res = jpyang.get_base_type(self.c)
        assert res == None, 'was: ' + res

        # A type statement without any children should also return None
        type_stmt = Statement(None, None, None, 'type', arg='string')
        res = jpyang.get_base_type(type_stmt)
        assert res == None, 'was: ' + res.arg

        # Adding the type statement as a child to l should work
        self.l.substmts.append(type_stmt)
        type_stmt.parent = self.l
        res = jpyang.get_base_type(self.l)
        assert res.arg == 'string', 'was: ' + res.arg

        # Calling with the container c (parent of l) should still return None
        res = jpyang.get_base_type(self.c)
        assert res == None, 'was: ' + res.arg

    def testRe_split(self):
        from re import split, findall
        res = filter(None, split('[< >,]', 'HashMap<Tagpath, SchemaNode>'))
        res2 = filter(None, findall(r'\w+', 'HashMap<Tagpath, SchemaNode>'))
        expected = ['HashMap', 'Tagpath', 'SchemaNode']
        assert res == expected, 'was: ' + str(res)
        assert res2 == res, 'was: ' + str(res2)


if __name__ == "__main__":
    """Launch all unit tests"""
    #import sys;sys.argv = ['', 'Test.testCapitalize_first']  # Only one
    unittest.main()
