/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.util.attribution

import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite

class AttributeTest extends CypherFunSuite {

  case class TestKey(override val id: Id) extends Identifiable

  class TestAttribute extends Attribute[TestKey, Int]

  test("test set") {
    val attr = new TestAttribute
    attr.set(Id(2), 33)

    attr.get(Id(2)) should be(33)
    a[Exception] should be thrownBy attr.get(Id(1))
    a[Exception] should be thrownBy attr.get(Id(3))
  }

  test("test isDefinedAt") {
    val attr = new TestAttribute
    attr.set(Id(2), 33)
    attr.set(Id(4), 27)

    attr.isDefinedAt(Id(1)) should be(false)
    attr.isDefinedAt(Id(2)) should be(true)
    attr.isDefinedAt(Id(3)) should be(false)
    attr.isDefinedAt(Id(4)) should be(true)

  }

  test("test iterator") {
    val attr = new TestAttribute
    attr.set(Id(2), 33)
    attr.set(Id(4), 27)

    val iter = attr.iterator
    iter.hasNext should be(true)
    iter.next() should be((Id(2),33))
    iter.hasNext should be(true)
    iter.next() should be((Id(4),27))
    iter.hasNext should be(false)
    intercept[NoSuchElementException](iter.next())
  }

  test("test empty iterator with hasNext") {
    val attr = new TestAttribute
    val iter = attr.iterator
    iter.hasNext should be(false)
    intercept[NoSuchElementException](iter.next())
  }

  test("test empty iterator without hasNext") {
    val attr = new TestAttribute
    val iter = attr.iterator
    intercept[NoSuchElementException](iter.next())
  }

  test("test size") {
    val attr = new TestAttribute
    attr.set(Id(2), 33)
    attr.set(Id(4), 27)
    attr.size should be(2)

    val attr2 = new TestAttribute
    attr2.size should be(0)
  }

}
