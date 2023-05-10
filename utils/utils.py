import json
import re
import traceback


def read_jar_json(version:int) -> dict:
    with open(f"./outputs/android-{version}/androidJar-{version}.json") as f:
        return json.loads(f.read())

def read_sdk_json(version:int) -> dict:
    with open(f"./outputs/android-{version}/android-{version}.json") as f:
        return json.loads(f.read())


def save_json(version:int, data:dict, name:str):
    with open(f"./outputs/android-{version}/{name}.json", "w") as f:
        f.write(json.dumps(data))



def getSimpleMethodName(name:str):
    name = delectT(name)
    return name[0:name.find("(")]

def delectT(msg:str):
    msg = msg.replace("->", "")
    try:
        stack = []
        result = []
        remove_char = set()
        for i, char in enumerate(msg):
            if char == "<":
                stack.append(i)
            elif char == ">":
                j = stack.pop()
                for k in range(j, i+1):
                    remove_char.add(k)
        for i, char in enumerate(msg):
            if i not in remove_char:
                result.append(char)
        return "".join(result)
    except:
        print(msg)
        traceback.print_exc()
        exit(0)

def extractT(msg:str):
    return re.findall("[A-Z][a-zA-Z]+", msg)


def get_words(name:str) -> list:

    words = []
    t1 = re.findall("[a-z]+|[A-Z][0-9]+|[A-Z][a-z]+", name)
    words.extend(t1)
    # print(name, words)
    return words
    # words = set()
    # i = start = end = 0
    # while i < len(name):
    #     if name[i] == ".":
    #         end = i
    #         words.add(name[start:end].lower())
    #         i += 1
    #         start = i
    #     elif 'A' <= name[i] <= 'Z' and (name[i - 1] != "."):
    #         end = i
    #         words.add(name[start:end].lower())
    #         start = i
    #         i += 1
    #     else:
    #         i += 1
    #     if i == len(words):
    #         end = i
    #         words.add(name[start:end].lower())

    return list(words)

if __name__ == '__main__':
    res = extractT("Coll<? extend <Hello>>")
    print(res)
    jm1 = "java.util.stream.StreamSupport.intStream(java.util.() -> Spliterators.spliterator(new BitSetIterator(), cardinality(), Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED).framework,int,boolean)"
    print("原始:", jm1.replace("->", ""))
    print("取出泛型", ":" ,delectT(jm1))
    jm2 = getSimpleMethodName( jm1)

    print(jm2)